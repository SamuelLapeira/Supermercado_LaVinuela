package com.lavinuela.shop.notifications.domain.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rellena los huecos {@code {{nombre}}} de una plantilla de correo.
 *
 * <p>En la versión HTML los valores se escapan, de modo que un nombre de usuario con caracteres especiales no puede
 * colar etiquetas en el correo. Un hueco sin valor se queda vacío.
 */
public final class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_]+)\\s*}}");

    private TemplateRenderer() {
    }

    public static String renderText(String template, Map<String, String> values) {
        return render(template, values, false);
    }

    public static String renderHtml(String template, Map<String, String> values) {
        return render(template, values, true);
    }

    private static String render(String template, Map<String, String> values, boolean escape) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.getOrDefault(matcher.group(1), "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(escape ? escapeHtml(value) : value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String escapeHtml(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
