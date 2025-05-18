package com.example.wifiredstone;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.UUID;

public class WiFiRedstoneMod implements ModInitializer {
    public static final String MOD_ID = "wifiredstone";

    // Реєстрація блоку та сутності
    public static final Block RECEIVER_BLOCK = new ReceiverBlock();
    public static final BlockEntityType<ReceiverBlockEntity> RECEIVER_BLOCK_ENTITY = BlockEntityType.Builder
            .create(ReceiverBlockEntity::new, RECEIVER_BLOCK)
            .build(null);
    public static final Item TRANSMITTER_ITEM = new TransmitterItem();
    public static final Identifier ACTIVATE_RECEIVER_PACKET = new Identifier(MOD_ID, "activate_receiver");

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "receiver"), RECEIVER_BLOCK);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "receiver"), RECEIVER_BLOCK_ENTITY);
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "transmitter"), TRANSMITTER_ITEM);
        registerServerPackets();
    }

    // Клас блоку Приймача
    public static class ReceiverBlock extends BlockWithEntity {
        public ReceiverBlock() {
            super(Settings.copy(Blocks.REPEATER).strength(0.8f));
        }

        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
            return new ReceiverBlockEntity(pos, state);
        }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient && player.getStackInHand(hand).isOf(TRANSMITTER_ITEM)) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof ReceiverBlockEntity receiver) {
                    receiver.syncWithTransmitter(player, player.getStackInHand(hand));
                    world.emitGameEvent(GameEvent.BLOCK_ACTIVATE, pos, GameEvent.Emitter.of(player, state));
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        }

        @Override
        public boolean emitsRedstonePower(BlockState state) {
            return true;
        }

        @Override
        public int getWeakRedstonePower(BlockState state, World world, BlockPos pos, Direction direction) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ReceiverBlockEntity receiver && receiver.isActive()) {
                return 15;
            }
            return 0;
        }

        @Override
        public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ReceiverBlockEntity receiver) {
                receiver.updateRedstoneOutput();
            }
        }
    }

    // Клас сутності блоку Приймача
    public static class ReceiverBlockEntity extends BlockEntity {
        private UUID syncedUUID;
        private int signalTicks = 0;

        public ReceiverBlockEntity(BlockPos pos, BlockState state) {
            super(RECEIVER_BLOCK_ENTITY, pos, state);
        }

        public void syncWithTransmitter(PlayerEntity player, ItemStack transmitter) {
            if (syncedUUID == null) {
                syncedUUID = UUID.randomUUID();
            }
            NbtCompound nbt = transmitter.getOrCreateNbt();
            nbt.putUuid("SyncedUUID", syncedUUID);
            transmitter.setNbt(nbt);
            markDirty();
        }

        public void activate() {
            if (signalTicks <= 0) {
                signalTicks = 20;
                updateRedstoneOutput();
            }
        }

        public boolean isActive() {
            return signalTicks > 0;
        }

        public void updateRedstoneOutput() {
            if (world != null && !world.isClient) {
                world.updateNeighbors(pos, getCachedState().getBlock());
            }
        }

        public UUID getSyncedUUID() {
            return syncedUUID;
        }

        @Override
        public void readNbt(NbtCompound nbt) {
            super.readNbt(nbt);
            if (nbt.containsUuid("SyncedUUID")) {
                syncedUUID = nbt.getUuid("SyncedUUID");
            }
            signalTicks = nbt.getInt("SignalTicks");
        }

        @Override
        protected void writeNbt(NbtCompound nbt) {
            super.writeNbt(nbt);
            if (syncedUUID != null) {
                nbt.putUuid("SyncedUUID", syncedUUID);
            }
            nbt.putInt("SignalTicks", signalTicks);
        }

        @Override
        public Packet<ClientPlayPacketListener> toUpdatePacket() {
            return BlockEntityUpdateS2CPacket.create(this);
        }

        @Override
        public NbtCompound toInitialChunkDataNbt() {
            return createNbt();
        }

        public static void tick(World world, BlockPos pos, BlockState state, ReceiverBlockEntity blockEntity) {
            if (blockEntity.signalTicks > 0) {
                blockEntity.signalTicks--;
                if (blockEntity.signalTicks <= 0) {
                    blockEntity.updateRedstoneOutput();
                }
                blockEntity.markDirty();
            }
        }
    }

    // Клас предмету Передавача
    public static class TransmitterItem extends Item {
        public TransmitterItem() {
            super(new Item.Settings().maxCount(1));
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.containsUuid("SyncedUUID")) {
                UUID syncedUUID = nbt.getUuid("SyncedUUID");
                if (!world.isClient) {
                    sendActivateReceiverPacket(user, syncedUUID, user.getBlockPos());
                }
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        }
    }

    // Мережеві пакети
    private static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(ACTIVATE_RECEIVER_PACKET, (server, player, handler, buf, responseSender) -> {
            UUID syncedUUID = buf.readUuid();
            BlockPos playerPos = buf.readBlockPos();
            server.execute(() -> {
                player.getWorld().getBlockEntities().stream()
                        .filter(be -> be instanceof ReceiverBlockEntity)
                        .map(be -> (ReceiverBlockEntity) be)
                        .filter(be -> syncedUUID.equals(be.getSyncedUUID()))
                        .findFirst()
                        .ifPresent(ReceiverBlockEntity::activate);
            });
        });
    }

    private static void sendActivateReceiverPacket(PlayerEntity player, UUID syncedUUID, BlockPos playerPos) {
        ServerPlayNetworking.send((ServerPlayerEntity) player, ACTIVATE_RECEIVER_PACKET, PacketByteBufs.create()
                .writeUuid(syncedUUID)
                .writeBlockPos(playerPos));
    }
}