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
    public abstract static class AbstractFireflyParticleData implements IParticleData {
        public final int fireflyId;

        public AbstractFireflyParticleData(int fireflyId) {
            this.fireflyId = fireflyId;
        }

        protected abstract ParticleType<?> particleType();

        @Override
        public ParticleType<?> getType() {
            return this.particleType();
        }

        @Override
        public void write(PacketBuffer buffer) {
            buffer.writeVarInt(this.fireflyId);
        }

        @Override
        public String getParameters() {
            return Integer.toString(this.fireflyId);
        }
    }

    public static class Dust extends AbstractFireflyParticleData {
        public Dust(int fireflyId) {
            super(fireflyId);
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

        @Override
        protected ParticleType<?> particleType() {
            return Registry.FIREFLY_DUST_PARTICLE.get();
        }
    }

    public static class Abdomen extends AbstractFireflyParticleData {
        public Abdomen(int fireflyId) {
            super(fireflyId);
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

        @Override
        protected ParticleType<?> particleType() {
            return Registry.FIREFLY_ABDOMEN_PARTICLE.get();
        }
    }
}
