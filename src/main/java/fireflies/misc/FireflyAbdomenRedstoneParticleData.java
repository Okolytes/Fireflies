package fireflies.misc;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import fireflies.init.Registry;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;

public class FireflyAbdomenRedstoneParticleData implements IParticleData {
    public final int fireflyId;

    public FireflyAbdomenRedstoneParticleData(int fireflyId) {
        this.fireflyId = fireflyId;
    }

    public static Codec<FireflyAbdomenRedstoneParticleData> fireflyAbdomenParticleDataCodec() {
        return Codec.INT.xmap(FireflyAbdomenRedstoneParticleData::new, (fireflyAbdomenParticleData) -> fireflyAbdomenParticleData.fireflyId);
    }

    public static ParticleType<FireflyAbdomenRedstoneParticleData> get() {
        return new ParticleType<FireflyAbdomenRedstoneParticleData>(true, new FireflyAbdomenRedstoneParticleData.Deserializer()) {
            @Override
            public Codec<FireflyAbdomenRedstoneParticleData> func_230522_e_() {
                return FireflyAbdomenRedstoneParticleData.fireflyAbdomenParticleDataCodec();
            }
        };
    }

    @Override
    public ParticleType<?> getType() {
        return Registry.FIREFLY_ABDOMEN_REDSTONE_PARTICLE.get();
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(fireflyId);
    }

    @Override
    public String getParameters() {
        return Integer.toString(fireflyId);
    }

    public static class Deserializer implements IDeserializer<FireflyAbdomenRedstoneParticleData> {
        @Override
        public FireflyAbdomenRedstoneParticleData deserialize(ParticleType<FireflyAbdomenRedstoneParticleData> type, StringReader rdr) throws CommandSyntaxException {
            rdr.skipWhitespace();
            return new FireflyAbdomenRedstoneParticleData(rdr.readInt());
        }

        @Override
        public FireflyAbdomenRedstoneParticleData read(ParticleType<FireflyAbdomenRedstoneParticleData> type, PacketBuffer buf) {
            return new FireflyAbdomenRedstoneParticleData(buf.readVarInt());
        }
    }
}