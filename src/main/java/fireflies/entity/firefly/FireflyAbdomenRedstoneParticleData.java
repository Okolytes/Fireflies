package fireflies.entity.firefly;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import fireflies.setup.FirefliesRegistration;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;

/**
 * To be the person sitting in chat and going CV paste, five seconds later CV paste.
 * Oh i got someone else copying and pasting same thing that i did. So CV paste again.
 * Paste it, paste it all day. Oh there is four-five that are pasting the same thing.
 * Lets spam it. I should start my own website of spammers.
 */
public class FireflyAbdomenRedstoneParticleData implements IParticleData {
    public final int fireflyId;

    public FireflyAbdomenRedstoneParticleData(int fireflyId) {
        this.fireflyId = fireflyId;
    }

    public static Codec<FireflyAbdomenRedstoneParticleData> fireflyAbdomenRedstoneParticleDataCodec() {
        return Codec.INT.xmap(FireflyAbdomenRedstoneParticleData::new, (fireflyAbdomenRedstoneParticleData) -> fireflyAbdomenRedstoneParticleData.fireflyId);
    }

    public static ParticleType<FireflyAbdomenRedstoneParticleData> get() {
        return new ParticleType<FireflyAbdomenRedstoneParticleData>(false, new FireflyAbdomenRedstoneParticleData.Deserializer()) {
            @Override
            public Codec<FireflyAbdomenRedstoneParticleData> func_230522_e_() {
                return FireflyAbdomenRedstoneParticleData.fireflyAbdomenRedstoneParticleDataCodec();
            }
        };
    }

    @Override
    public ParticleType<?> getType() {
        return FirefliesRegistration.FIREFLY_ABDOMEN_REDSTONE_PARTICLE.get();
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeVarInt(fireflyId);
    }

    @Override
    public String getParameters() {
        return Integer.toString(fireflyId);
    }

    public static class Deserializer implements IParticleData.IDeserializer<FireflyAbdomenRedstoneParticleData> {
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
