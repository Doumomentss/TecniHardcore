package net.tecnihardcore;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SocialStoreTest {
    static String id(){return UUID.randomUUID().toString();}
    static SocialStore.Team team(String owner){var t=new SocialStore.Team();t.id=id();t.owner=owner;t.name="Cristal";t.members.add(owner);return t;}
    @Test void emptyAndHistoricalTruceLedgerLoad(){var b=new SocialStore.Business();var t=new SocialStore.Truce();t.expelled=id();t.team=id();t.members.add(id());b.truces.add(t);assertDoesNotThrow(()->SocialStore.validate(b));}
    @Test void identityMismatchFailsClosed(){var b=new SocialStore.Business();var t=team(id());b.teams.put(id(),t);assertThrows(IllegalStateException.class,()->SocialStore.validate(b));}
    @Test void duplicateMembershipRejected(){var b=new SocialStore.Business();String owner=id();var t=team(owner);b.teams.put(t.id,t);var second=team(owner);b.teams.put(second.id,second);assertThrows(IllegalStateException.class,()->SocialStore.validate(b));}
    @Test void negativeWalletRejected(){var b=new SocialStore.Business();b.wallets.put(id(),-1L);assertThrows(IllegalArgumentException.class,()->SocialStore.validate(b));}
    @Test void invalidQuestRejected(){var b=new SocialStore.Business();b.quests.put(id(),Set.of("admin:grant"));assertThrows(IllegalStateException.class,()->SocialStore.validate(b));}
    @Test void roundtripPreservesOfflineProtectionAndWallet(){var b=new SocialStore.Business();String owner=id();var t=team(owner);b.teams.put(t.id,t);b.wallets.put(owner,80L);var truce=new SocialStore.Truce();truce.team=t.id;truce.expelled=id();truce.name=t.name;truce.members.add(owner);truce.remaining=7137;b.truces.add(truce);var read=SocialStore.JSON.fromJson(SocialStore.JSON.toJson(b),SocialStore.Business.class);SocialStore.validate(read);assertEquals(80L,read.wallets.get(owner));assertEquals(7137,read.truces.get(0).remaining);}
    @Test void invalidOfferCannotCreateMoney(){var b=new SocialStore.Business();var o=new SocialStore.Offer();o.id=id();o.seller=id();o.item="{id:\"minecraft:diamond\",Count:1b}";o.price=0;b.offers.put(o.id,o);assertThrows(IllegalStateException.class,()->SocialStore.validate(b));}
}
