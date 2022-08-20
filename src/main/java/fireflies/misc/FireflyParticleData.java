package fireflies.misc;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import fireflies.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

@SuppressWarnings("deprecation")
public class FireflyParticleData {
    public abstract static class AbstractFireflyParticleData implements ParticleOptions {
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
        public void writeToNetwork(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.fireflyId);
        }

        @Override
        public String writeToString() {
            return Integer.toString(this.fireflyId);
        }
    }

    public static class Dust extends AbstractFireflyParticleData {
        public Dust(int fireflyId) {
            super(fireflyId);
        }

        public static ParticleType<Dust> get() {
            return new ParticleType<Dust>(true, new Deserializer<Dust>() {
                @Override
                public Dust fromCommand(ParticleType<Dust> type, StringReader reader) throws CommandSyntaxException {
                    reader.skipWhitespace();
                    return new Dust(reader.readInt());
                }

                @Override
                public Dust fromNetwork(ParticleType<Dust> type, FriendlyByteBuf buffer) {
                    return new Dust(buffer.readVarInt());
                }
            }) {
                @Override
                public Codec<Dust> codec() {
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
            return new ParticleType<Abdomen>(true, new Deserializer<Abdomen>() {
                @Override
                public Abdomen fromCommand(ParticleType<Abdomen> type, StringReader reader) throws CommandSyntaxException {
                    reader.skipWhitespace();
                    return new Abdomen(reader.readInt());
                }

                @Override
                public Abdomen fromNetwork(ParticleType<Abdomen> type, FriendlyByteBuf buffer) {
                    return new Abdomen(buffer.readVarInt());
                }
            }) {
                @Override
                public Codec<Abdomen> codec() {
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
