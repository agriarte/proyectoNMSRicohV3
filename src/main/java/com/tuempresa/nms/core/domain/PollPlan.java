package com.tuempresa.nms.core.domain;

import java.util.List;
import java.util.Set;

public record PollPlan(
    Set<String> scalarOids,
    List<TableWalk> tableWalks,
    boolean needsColor,
    boolean needsScanner
) {
    public PollPlan {
        if (scalarOids == null) scalarOids = Set.of();
        if (tableWalks == null) tableWalks = List.of();
    }
}
