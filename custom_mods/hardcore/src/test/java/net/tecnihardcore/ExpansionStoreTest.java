package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ExpansionStoreTest {
    private ExpansionStore.Data fixture(){var data=new ExpansionStore.Data();var m=new ExpansionStore.Mount();m.tier=3;m.health=47;data.mounts.put(UUID.randomUUID().toString(),m);return data;}
    @Test void oldRecordsWithoutReturnFieldsRemainValid(){assertDoesNotThrow(()->ExpansionStore.validate(fixture()));}
    @Test void invalidHealthCannotBecomeAnotherMount(){var d=fixture();d.mounts.values().iterator().next().health=Float.NaN;assertThrows(IllegalStateException.class,()->ExpansionStore.validate(d));}
    @Test void excessiveHealthFailsClosed(){var d=fixture();d.mounts.values().iterator().next().health=56;assertThrows(IllegalStateException.class,()->ExpansionStore.validate(d));}
    @Test void activeReferenceRequiresEntityAndOwner(){var d=fixture();d.mounts.values().iterator().next().state="activa";assertThrows(IllegalStateException.class,()->ExpansionStore.validate(d));}
    @Test void rewardCannotReferenceMissingCreature(){var d=fixture();var r=new ExpansionStore.Reward();r.player=UUID.randomUUID().toString();r.mount=UUID.randomUUID().toString();d.rewards.put("encounter",r);assertThrows(IllegalStateException.class,()->ExpansionStore.validate(d));}
    @Test void negativeRewardFailsClosed(){var d=fixture();var r=new ExpansionStore.Reward();r.player=UUID.randomUUID().toString();r.diamonds=-1;d.rewards.put("encounter",r);assertThrows(IllegalStateException.class,()->ExpansionStore.validate(d));}
    @Test void returnPositionRequiresRealIdentity(){var d=fixture();var m=d.mounts.values().iterator().next();m.returnPending=true;m.returnY=95;assertThrows(IllegalStateException.class,()->ExpansionStore.validate(d));}
}
