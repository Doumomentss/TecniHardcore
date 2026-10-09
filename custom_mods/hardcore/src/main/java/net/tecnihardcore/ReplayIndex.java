package net.tecnihardcore;

import java.util.*;

/** Stable per-player display IDs backed by the existing global evidence ledger. */
final class ReplayIndex {
    private ReplayIndex() {}

    static List<ModerationStore.Clip> history(Collection<ModerationStore.Clip> clips, String name) {
        Set<String> players=new HashSet<>();
        for(var clip:clips)if(clip.name.equalsIgnoreCase(name))players.add(clip.player);
        if(players.size()!=1)return List.of();
        String player=players.iterator().next();
        return clips.stream().filter(c->c.player.equals(player))
            .sorted(Comparator.comparingInt(c->Integer.parseInt(c.id))).toList();
    }

    static boolean ambiguous(Collection<ModerationStore.Clip> clips,String name) {
        return clips.stream().filter(c->c.name.equalsIgnoreCase(name)).map(c->c.player).distinct().limit(2).count()>1;
    }
}
