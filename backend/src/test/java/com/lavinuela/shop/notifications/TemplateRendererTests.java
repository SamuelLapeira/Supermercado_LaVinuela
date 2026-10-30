package com.lavinuela.shop.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavinuela.shop.notifications.domain.service.TemplateRenderer;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateRendererTests {

    @Test
    void fillsThePlaceholders() {
        assertThat(TemplateRenderer.renderText("Hola, {{customerName}}. Código: {{ code }}.",
                Map.of("customerName", "ana", "code", "123456"))).isEqualTo("Hola, ana. Código: 123456.");
    }

    @Test
    void leavesMissingPlaceholdersEmpty() {
        assertThat(TemplateRenderer.renderText("A{{nada}}B", Map.of())).isEqualTo("AB");
    }

    @Test
    void escapesValuesInHtmlSoTheyCannotInjectTags() {
        String html = TemplateRenderer.renderHtml("<p>Hola, {{customerName}}</p>",
                Map.of("customerName", "<script>alert(\"x\")</script>&'"));

        assertThat(html).isEqualTo("<p>Hola, &lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;&amp;&#39;</p>");
    }

    @Test
    void doesNotEscapeInPlainText() {
        assertThat(TemplateRenderer.renderText("{{v}}", Map.of("v", "a & b <c>"))).isEqualTo("a & b <c>");
    }

    @Test
    void valuesWithDollarSignsAndBackslashesAreInsertedLiterally() {
        assertThat(TemplateRenderer.renderText("{{v}}", Map.of("v", "$1 \\ $&"))).isEqualTo("$1 \\ $&");
    }
}
