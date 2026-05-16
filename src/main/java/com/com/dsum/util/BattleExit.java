package com.com.dsum.util;

import java.util.Map;

public record BattleExit(double newDSum, Map<Integer, Integer> suggestions, double entryDelta, double mean) {
}
