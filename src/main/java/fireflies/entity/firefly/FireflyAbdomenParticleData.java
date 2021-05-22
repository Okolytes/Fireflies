package fireflies.entity.firefly;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import fireflies.setup.FirefliesRegistration;

public class FireflyAbdomenParticleData implements IParticleData {
    public final int fireflyId;

    public FireflyAbdomenParticleData(int fireflyId) {
        this.fireflyId = fireflyId;
    }

    public static Codec<FireflyAbdomenParticleData> fireflyAbdomenParticleDataCodec() {
        return Codec.INT.xmap(FireflyAbdomenParticleData::new, (fireflyAbdomenParticleData) -> fireflyAbdomenParticleData.fireflyId);
    }

    public static ParticleType<FireflyAbdomenParticleData> get() {
        return new ParticleType<FireflyAbdomenParticleData>(false, new FireflyAbdomenParticleData.Deserializer()) {
            @Override
            public Codec<FireflyAbdomenParticleData> func_230522_e_() {
                return FireflyAbdomenParticleData.fireflyAbdomenParticleDataCodec();
            }
        };
    }

    @Override
    public ParticleType<?> getType() {
        return FirefliesRegistration.FIREFLY_ABDOMEN_PARTICLE.get();
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(fireflyId);
    }

    @Override
    public String getParameters() {
        return Integer.toString(fireflyId);
    }

    public static class Deserializer implements IParticleData.IDeserializer<FireflyAbdomenParticleData> {
        @Override
        public FireflyAbdomenParticleData deserialize(ParticleType<FireflyAbdomenParticleData> type, StringReader rdr) throws CommandSyntaxException {
            rdr.skipWhitespace();
            return new FireflyAbdomenParticleData(rdr.readInt());
        }

        @Override
        public FireflyAbdomenParticleData read(ParticleType<FireflyAbdomenParticleData> type, PacketBuffer buf) {
            return new FireflyAbdomenParticleData(buf.readVarInt());
        }
    }
}
