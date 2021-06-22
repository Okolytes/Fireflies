package fireflies.misc;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import fireflies.Registry;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;

// This is the worstest java code of All Time
@SuppressWarnings("deprecation")
public class FireflyParticleData {
    private abstract static class AbstractFireflyParticleData implements IParticleData {
        public final int fireflyId;

        public AbstractFireflyParticleData(int fireflyId) {
            this.fireflyId = fireflyId;
        }

        protected abstract ParticleType<?> particleType();

        @Override
        public ParticleType<?> getType() {
            return particleType();
        }

        @Override
        public void write(PacketBuffer buffer) {
            buffer.writeVarInt(fireflyId);
        }

        @Override
        public String getParameters() {
            return Integer.toString(fireflyId);
        }
    }

    public static class Dust extends AbstractFireflyParticleData {
        public Dust(int fireflyId) {
            super(fireflyId);
        }

        @Override
        protected ParticleType<?> particleType() {
            return Registry.FIREFLY_DUST_PARTICLE.get();
        }

        public static ParticleType<Dust> get() {
            return new ParticleType<Dust>(true, new IDeserializer<Dust>() {
                @Override
                public Dust deserialize(ParticleType<Dust> type, StringReader reader) throws CommandSyntaxException {
                    reader.skipWhitespace();
                    return new Dust(reader.readInt());
                }

                @Override
                public Dust read(ParticleType<Dust> type, PacketBuffer buffer) {
                    return new Dust(buffer.readVarInt());
                }
            }) {
                @Override
                public Codec<Dust> func_230522_e_() {
                    return Codec.INT.xmap(Dust::new, (fireflyDustParticleData) -> fireflyDustParticleData.fireflyId);
                }
            };
        }
    }

    public static class DustRedstone extends AbstractFireflyParticleData {
        public DustRedstone(int fireflyId) {
            super(fireflyId);
        }

        @Override
        protected ParticleType<?> particleType() {
            return Registry.FIREFLY_DUST_REDSTONE_PARTICLE.get();
        }

        public static ParticleType<DustRedstone> get() {
            return new ParticleType<DustRedstone>(true, new IDeserializer<DustRedstone>() {
                @Override
                public DustRedstone deserialize(ParticleType<DustRedstone> type, StringReader reader) throws CommandSyntaxException {
                    reader.skipWhitespace();
                    return new DustRedstone(reader.readInt());
                }

                @Override
                public DustRedstone read(ParticleType<DustRedstone> type, PacketBuffer buffer) {
                    return new DustRedstone(buffer.readVarInt());
                }
            }) {
                @Override
                public Codec<DustRedstone> func_230522_e_() {
                    return Codec.INT.xmap(DustRedstone::new, (fireflyDustParticleData) -> fireflyDustParticleData.fireflyId);
                }
            };
        }
    }

    public static class Abdomen extends AbstractFireflyParticleData {
        public Abdomen(int fireflyId) {
            super(fireflyId);
        }

        @Override
        protected ParticleType<?> particleType() {
            return Registry.FIREFLY_ABDOMEN_PARTICLE.get();
        }

        public static ParticleType<Abdomen> get() {
            return new ParticleType<Abdomen>(true, new IDeserializer<Abdomen>() {
                @Override
                public Abdomen deserialize(ParticleType<Abdomen> type, StringReader reader) throws CommandSyntaxException {
                    reader.skipWhitespace();
                    return new Abdomen(reader.readInt());
                }

                @Override
                public Abdomen read(ParticleType<Abdomen> type, PacketBuffer buffer) {
                    return new Abdomen(buffer.readVarInt());
                }
            }) {
                @Override
                public Codec<Abdomen> func_230522_e_() {
                    return Codec.INT.xmap(Abdomen::new, (fireflyDustParticleData) -> fireflyDustParticleData.fireflyId);
                }
            };
        }
    }

    public static class AbdomenRedstone extends AbstractFireflyParticleData {

        public AbdomenRedstone(int fireflyId) {
            super(fireflyId);
        }

        @Override
        protected ParticleType<?> particleType() {
            return Registry.FIREFLY_ABDOMEN_REDSTONE_PARTICLE.get();
        }

        public static ParticleType<AbdomenRedstone> get() {
            return new ParticleType<AbdomenRedstone>(true, new IDeserializer<AbdomenRedstone>() {
                @Override
                public AbdomenRedstone deserialize(ParticleType<AbdomenRedstone> type, StringReader reader) throws CommandSyntaxException {
                    reader.skipWhitespace();
                    return new AbdomenRedstone(reader.readInt());
                }

                @Override
                public AbdomenRedstone read(ParticleType<AbdomenRedstone> type, PacketBuffer buffer) {
                    return new AbdomenRedstone(buffer.readVarInt());
                }
            }) {
                @Override
                public Codec<AbdomenRedstone> func_230522_e_() {
                    return Codec.INT.xmap(AbdomenRedstone::new, (fireflyDustParticleData) -> fireflyDustParticleData.fireflyId);
                }
            };
        }

    }

    public static class AbdomenIllumerin extends AbstractFireflyParticleData {
        public AbdomenIllumerin(int fireflyId) {
            super(fireflyId);
        }

        @Override
        protected ParticleType<?> particleType() {
            return Registry.FIREFLY_ABDOMEN_ILLUMERIN_PARTICLE.get();
        }

        public static ParticleType<AbdomenIllumerin> get() {
            return new ParticleType<AbdomenIllumerin>(true, new IDeserializer<AbdomenIllumerin>() {
                @Override
                public AbdomenIllumerin deserialize(ParticleType<AbdomenIllumerin> type, StringReader reader) throws CommandSyntaxException {
                    reader.skipWhitespace();
                    return new AbdomenIllumerin(reader.readInt());
                }

                @Override
                public AbdomenIllumerin read(ParticleType<AbdomenIllumerin> type, PacketBuffer buffer) {
                    return new AbdomenIllumerin(buffer.readVarInt());
                }
            }) {
                @Override
                public Codec<AbdomenIllumerin> func_230522_e_() {
                    return Codec.INT.xmap(AbdomenIllumerin::new, (fireflyDustParticleData) -> fireflyDustParticleData.fireflyId);
                }
            };
        }
    }
}
