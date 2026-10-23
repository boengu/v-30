package com.example.phonehud;

/** Distinguish a schematic fork/exit/tunnel from ordinary turns; no lane-level claims. */
public final class DiagramClassifier {
    private DiagramClassifier() { }
    public static String select(String text, String direction) {
        if (!"LEFT".equals(direction) && !"RIGHT".equals(direction)) return direction;
        String s = text == null ? "" : text.replaceAll("\\s+", " ");
        String side = "LEFT".equals(direction) ? "LEFT" : "RIGHT";
        if (s.matches("(?s).*(터널|지하차도|지하도로).*(출구|나와|빠지|진출|방향|분기|진입).*"))
            return "TUNNEL_" + side;
        if (s.matches("(?s).*(터널|지하차도|지하도로).*") && s.matches("(?s).*(좌|우|왼쪽|오른쪽).*(?:진입|분기|진출|빠지|방향).*") )
            return "TUNNEL_" + side;
        if (s.matches("(?s).*(?:IC|I\\.C\\.|나들목|출구|램프|진출).*") && s.matches("(?s).*(?:분기|진출|나가|빠지|진입|출구).*") )
            return "EXIT_" + side;
        if (s.matches("(?s).*(?:JC|JCT|분기점|갈림길|분기|합류|갈라지는|고속도로).*") )
            return "FORK_" + side;
        return side;
    }
}
