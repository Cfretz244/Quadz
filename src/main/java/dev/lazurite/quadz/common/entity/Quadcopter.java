package dev.lazurite.quadz.common.entity;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import dev.lazurite.form.api.Templated;
import dev.lazurite.form.api.loader.TemplateLoader;
import dev.lazurite.quadz.Quadz;
import dev.lazurite.quadz.client.render.QuadcopterView;
import dev.lazurite.quadz.common.util.Bindable;
import dev.lazurite.quadz.common.util.Search;
import dev.lazurite.quadz.common.item.GogglesItem;
import dev.lazurite.quadz.common.item.RemoteItem;
import dev.lazurite.quadz.common.util.Matrix4fHelper;
import dev.lazurite.quadz.common.util.RateProfile;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.ElementRigidBody;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import dev.lazurite.rayon.impl.bullet.math.Convert;
import dev.lazurite.toolbox.api.math.QuaternionHelper;
import dev.lazurite.toolbox.api.math.VectorHelper;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import java.util.Optional;

public class Quadcopter extends LivingEntity implements EntityPhysicsElement, Templated, GeoEntity, Bindable {

    public static final EntityDataAccessor<String> TEMPLATE = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> PREV_TEMPLATE = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> BIND_ID = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> CAMERA_ANGLE = SynchedEntityData.defineId(Quadcopter.class, EntityDataSerializers.INT);

    // Underwater the drone uses the SIMPLE air-drag term at this fraction of its in-air strength
    // (with Rayon's heavy water drag suppressed), so water feels like a damped version of normal
    // flight rather than molasses or frictionless. First-pass value; tune by feel.
    public static final float UNDERWATER_AIR_DRAG_FRACTION = 0.15f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final EntityRigidBody rigidBody = new EntityRigidBody(this);
    private final QuadcopterView view = new QuadcopterView(this);

    // Server-side, transient: tracks whether a pilot was viewing last tick so we auto-arm only on
    // view entry (after that the explicit arm/disarm toggle owns the ARMED state).
    private boolean wasViewed = false;

    // Server-side, transient: a camera angle to restore on placement (from a picked-up drone item),
    // honored by ServerEventHooks.onEntityTemplateChanged instead of the template default. Consumed once.
    private Integer pendingCameraAngle = null;

    // Server-side, transient: last tick's submersion state, so we only reconfigure the body's drag on
    // a water<->air transition instead of every tick (setDragCoefficient re-dirties body properties).
    private boolean wasInWater = false;

    public Quadcopter(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
        // 1.21.2: Entity.noCulling is gone; culling is disabled via
        // QuadcopterEntityRenderer.affectedByCulling instead.
        this.rigidBody.setBuoyancyType(ElementRigidBody.BuoyancyType.NONE);
        this.rigidBody.setDragType(ElementRigidBody.DragType.SIMPLE);
    }

    @Override
    public void tick() {
        super.tick();

        // Update template if a change is detected
        if (!getTemplate().equals(getEntityData().get(PREV_TEMPLATE))) {
            getEntityData().set(PREV_TEMPLATE, getTemplate());
            this.refreshDimensions();
        }

        if (!this.level().isClientSide()) {
            // Server-side only prioritization
            Optional.ofNullable(getRigidBody().getPriorityPlayer()).ifPresent(player -> {
                if (!((ServerPlayer) player).getCamera().equals(this)) {
                    getRigidBody().prioritize(null);
                }
            });

            // Break grass, flowers, etc if the quadcopter is above a certain weight.
            if (this.getRigidBody().getMass() > 0.1f) {
                var block = this.level().getBlockState(this.blockPosition()).getBlock();

                if (block instanceof BushBlock || block instanceof VineBlock) {
                    this.level().destroyBlock(this.blockPosition(), false, this);
                }
            }

            // Hurt entities on collision
            this.level().getEntities(this, this.getBoundingBox(), entity -> entity instanceof LivingEntity).forEach(entity -> {
                entity.hurt(this.damageSources().flyIntoWall(), 2.0f);
            });

            // Submersible handling: underwater the drone should feel like a damped version of normal
            // flight — not molasses (full water drag) and not frictionless. Rayon's water drag ignores
            // the drag coefficient once its mass-based stopping-force clamp kicks in, so we suppress the
            // water drag entirely (waterDragScale 0) and instead lean on the SIMPLE air-drag term at a
            // reduced coefficient, giving ~UNDERWATER_AIR_DRAG_FRACTION of the in-air drag. Out of water
            // we restore full water drag and the template coefficient. Body stays on DragType.SIMPLE
            // throughout; we only touch the setters on a transition (setDragCoefficient re-dirties props).
            final boolean inWater = this.isInWater();
            if (inWater != this.wasInWater) {
                TemplateLoader.getTemplateById(this.getTemplate()).ifPresent(template -> {
                    final float baseDrag = template.metadata().get("dragCoefficient").getAsFloat();
                    this.getRigidBody().setWaterDragScale(inWater ? 0.0f : 1.0f);
                    this.getRigidBody().setDragCoefficient(inWater ? baseDrag * UNDERWATER_AIR_DRAG_FRACTION : baseDrag);
                });
                this.wasInWater = inWater;
            }
        }

        Search.forPlayer(this).ifPresentOrElse(player -> {
            player.quadz$syncJoystick();

            if (!this.level().isClientSide()) {
                // Auto-arm when the pilot first enters the view; after that the explicit arm/disarm
                // toggle owns the state, so we don't force it back to armed every tick.
                if (!this.wasViewed) {
                    this.setArmed(true);
                    this.wasViewed = true;
                }

                if (player instanceof ServerPlayer serverPlayer && serverPlayer.getCamera() == this && !player.equals(this.getRigidBody().getPriorityPlayer())) {
                    this.getRigidBody().prioritize(player);
                }
            }

            var pitch = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "pitch"));
            var yaw = -1 * player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "yaw"));
            var roll = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "roll"));
            var throttle = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "throttle")) + 1.0f;

            // Per-axis rate params. The client resolves link-vs-per-axis and pushes these, so here
            // we just read each axis's own keys (linked simply means all three carry equal values).
            var pitchRate = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "pitch_rate"));
            var pitchSuperRate = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "pitch_super_rate"));
            var pitchExpo = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "pitch_expo"));
            var yawRate = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "yaw_rate"));
            var yawSuperRate = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "yaw_super_rate"));
            var yawExpo = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "yaw_expo"));
            var rollRate = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "roll_rate"));
            var rollSuperRate = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "roll_super_rate"));
            var rollExpo = player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "roll_expo"));

            // The selected rate system is synced as a joystick "axis" (its ordinal); guard the
            // index in case it hasn't synced yet.
            var profiles = RateProfile.values();
            var profileIndex = Math.round(player.quadz$getJoystickValue(Identifier.fromNamespaceAndPath(Quadz.MODID, "rate_profile")));
            var profile = profiles[Math.max(0, Math.min(profiles.length - 1, profileIndex))];

            // Motors only respond while armed. Disarmed = no control authority or thrust (and the
            // GeckoLib controller stops the props), so the drone just falls/coasts on physics.
            if (this.isArmed()) {
                this.rotate(
                        (float) profile.calculate(pitch, pitchRate, pitchSuperRate, pitchExpo, 0.05f),
                        (float) profile.calculate(yaw, yawRate, yawSuperRate, yawExpo, 0.05f),
                        (float) profile.calculate(roll, rollRate, rollSuperRate, rollExpo, 0.05f)
                );

                // Decrease angular velocity
                if (throttle > 0.1f) {
                    var correction = getRigidBody().getAngularVelocity(new Vector3f()).multLocal(0.5f * throttle);

                    if (Float.isFinite(correction.lengthSquared())) {
                        getRigidBody().setAngularVelocity(correction);
                    }
                }

                // Get the thrust unit vector
                // TODO make this into it's own class
                var mat = new Matrix4f();
                Matrix4fHelper.fromQuaternion(mat, QuaternionHelper.rotateX(Convert.toMinecraft(getRigidBody().getPhysicsRotation(new Quaternion())), 90));
                var unit = Convert.toBullet(Matrix4fHelper.matrixToVector(mat));

                // Calculate basic thrust
                var thrust = new Vector3f().set(unit).multLocal((float) (getThrust() * (Math.pow(throttle, getThrustCurve()))));

                // Calculate thrust from yaw spin
                var yawThrust = new Vector3f().set(unit).multLocal(Math.abs(yaw * getThrust() * 0.002f));

                // Add up the net thrust and apply the force
                if (Float.isFinite(thrust.length())) {
                    getRigidBody().applyCentralForce(thrust.add(yawThrust).multLocal(-1));
                } else {
                    Quadz.LOGGER.warn("Infinite thrust force!");
                }
            }
        }, () -> {
            if (!this.level().isClientSide()) {
                this.setArmed(false);
                this.wasViewed = false;
                this.getRigidBody().prioritize(null);
            }
        });
    }

    public float getThrust() {
        return TemplateLoader.getTemplateById(this.getTemplate())
                .map(template -> template.metadata().get("thrust").getAsFloat())
                .orElse(0.0f);
    }

    public float getThrustCurve() {
        return TemplateLoader.getTemplateById(this.getTemplate())
                .map(template -> template.metadata().get("thrustCurve").getAsFloat())
                .orElse(0.0f);
    }

    public void rotate(float x, float y, float z) {
        var rot = new Quaternionf(0, 0, 0, 1);
        QuaternionHelper.rotateX(rot, x);
        QuaternionHelper.rotateY(rot, y);
        QuaternionHelper.rotateZ(rot, z);

        var trans = getRigidBody().getTransform(new Transform());
        trans.getRotation().set(trans.getRotation().mult(Convert.toBullet(rot)));
        getRigidBody().setPhysicsTransform(trans);
    }

    @Override
    // 26.1: interact gained the hit location.
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!level().isClientSide()) {
            final var stack = player.getInventory().getSelectedItem();

            if (stack.getItem() instanceof RemoteItem) {
                Bindable.get(stack).ifPresent(bindable -> Bindable.bind(this, bindable));
                player/*26.1*/.sendOverlayMessage(Component.translatable("quadz.message.bound"));
            }
        }

        return InteractionResult.SUCCESS;
    }

    // 1.21.2: kill/spawnAtLocation take the ServerLevel; hurt() split into hurtServer/hurtClient.
    @Override
    public void kill(ServerLevel level) {
        var itemStack = new ItemStack(Quadz.QUADCOPTER_ITEM);
        Bindable.get(itemStack).ifPresent(bindable -> bindable.copyFrom(this));
        Templated.get(itemStack).copyFrom(this);
        // Preserve the adjusted camera uptilt on the dropped item so it survives re-placement.
        itemStack.set(dev.lazurite.quadz.common.util.QuadzComponents.CAMERA_ANGLE, this.getEntityData().get(CAMERA_ANGLE));
        this.spawnAtLocation(level, itemStack);
        this.remove(RemovalReason.KILLED);
    }

    @Override
    public boolean hurtServer(ServerLevel level, @NotNull DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer) {
            this.kill(level);
            return true;
        }

        return false;
    }

    // 1.21.6: entity save data flows through ValueInput/ValueOutput instead of CompoundTag.
    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        getEntityData().set(TEMPLATE, input.getStringOr("template", ""));
        getEntityData().set(BIND_ID, input.getIntOr("bind_id", 0));

        // Restore the saved uptilt across sessions. Setting CAMERA_ANGLE alone isn't enough: on the
        // first tick after load the template-change handler (ServerEventHooks.onEntityTemplateChanged)
        // fires — because PREV_TEMPLATE resets to "" — and, finding no pending angle, resets CAMERA_ANGLE
        // to the template DEFAULT, wiping the loaded value. So we also stage it as the pending angle
        // (the same channel item placement uses), which that handler consumes instead of the default.
        final int savedAngle = input.getIntOr("camera_angle", 0);
        getEntityData().set(CAMERA_ANGLE, savedAngle);
        setPendingCameraAngle(savedAngle);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("template", getTemplate());
        output.putInt("bind_id", getEntityData().get(BIND_ID));
        output.putInt("camera_angle", getEntityData().get(CAMERA_ANGLE));
    }

    @Override
    public Vec3 getPosition(float tickDelta) {
        return VectorHelper.toVec3(Convert.toMinecraft(getPhysicsLocation(new Vector3f(), tickDelta)));
    }

    @Override
    public float getViewYRot(float tickDelta) {
        return QuaternionHelper.getYaw(Convert.toMinecraft(getPhysicsRotation(new Quaternion(), tickDelta)));
    }

    @Override
    public float getViewXRot(float tickDelta) {
        return QuaternionHelper.getPitch(Convert.toMinecraft(getPhysicsRotation(new Quaternion(), tickDelta)));
    }

    @Override
    public Direction getDirection() {
        return Direction.fromYRot(QuaternionHelper.getYaw(Convert.toMinecraft(getPhysicsRotation(new Quaternion(), 1.0f))));
    }

    // 1.21: defineSynchedData receives a SynchedEntityData.Builder and registers via builder.define(...).
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TEMPLATE, "");
        builder.define(PREV_TEMPLATE, "");
        builder.define(ARMED, false);
        builder.define(BIND_ID, 0);
        builder.define(CAMERA_ANGLE, 0);
    }

//    @Override
//    public boolean shouldRenderPlayer() {
//        return true;
//    }

//    @Override
    public boolean shouldPlayerBeViewing(Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GogglesItem;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot equipmentSlot) {
        return new ItemStack(Items.AIR);
    }

    @Override
    public void setItemSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {

    }

    @Override
    public HumanoidArm getMainArm() {
        return null;
    }

    @Override
    public EntityRigidBody getRigidBody() {
        return this.rigidBody;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<Quadcopter>("base", 0, state -> {
            if (this.isArmed()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("armed"));
            }

            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void setBindId(int bindId) {
        this.getEntityData().set(BIND_ID, bindId);
    }

    @Override
    public int getBindId() {
        return this.getEntityData().get(BIND_ID);
    }

    @Override
    public String getTemplate() {
        return this.getEntityData().get(TEMPLATE);
    }

    @Override
    public void setTemplate(String template) {
        this.getEntityData().set(TEMPLATE, template);
    }

    /** Queue a camera uptilt to restore on placement (used when a drone item carries a saved angle). */
    public void setPendingCameraAngle(int angle) {
        this.pendingCameraAngle = angle;
    }

    /** Returns the pending camera angle (or null) and clears it. */
    public Integer consumePendingCameraAngle() {
        var angle = this.pendingCameraAngle;
        this.pendingCameraAngle = null;
        return angle;
    }

    public void setArmed(boolean armed) {
        this.getEntityData().set(ARMED, armed);
    }

    public boolean isArmed() {
        return this.getEntityData().get(ARMED);
    }

    public QuadcopterView getView() {
        return this.view;
    }

}
