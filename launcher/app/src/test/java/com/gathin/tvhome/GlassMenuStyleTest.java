package com.gathin.tvhome;

import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GlassMenuStyleTest {
    @Test public void oversizedMenusCanScrollInsteadOfClippingActions() throws Exception {
        Element panel = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new File("src/main/res/layout/popup_context_menu.xml")).getDocumentElement();
        assertEquals("ScrollView", panel.getTagName());
        assertEquals(1, panel.getElementsByTagName("LinearLayout").getLength());
    }

    @Test public void glassLabelsRemainReadableOverLightAndDarkArtwork() throws Exception {
        for (String selector : new String[]{"pill_text_dim", "pill_text_mid"}) {
            Element focused = (Element) DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new File("src/main/res/drawable/" + selector + ".xml"))
                    .getElementsByTagName("item").item(0);
            assertEquals("true", focused.getAttribute("android:state_focused"));
            assertEquals("@color/glass_text_secondary_focused", focused.getAttribute("android:color"));
        }
        Map<String, Integer> palette = new HashMap<>();
        NodeList colors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new File("src/main/res/values/colors.xml")).getElementsByTagName("color");
        for (int index = 0; index < colors.getLength(); index++) {
            Element color = (Element) colors.item(index);
            palette.put(color.getAttribute("name"), (int) Long.parseLong(color.getTextContent().trim().substring(1), 16));
        }
        for (int backdrop : new int[]{0xFF000000, 0xFFFFFFFF}) {
            for (String endpoint : new String[]{"top", "bottom"}) {
                int surface = blend(palette.get("glass_surface_" + endpoint), backdrop);
                readable(palette.get("glass_text_primary"), surface);
                readable(palette.get("glass_text_secondary"), surface);
                int focus = blend(palette.get("glass_focus_" + endpoint), surface);
                readable(palette.get("glass_text_primary"), focus);
                readable(palette.getOrDefault("glass_text_secondary_focused", palette.get("glass_text_secondary")), focus);
                readable(palette.get("glass_text_primary"),
                        blend(palette.get("glass_pressed_" + endpoint), surface));
                readable(palette.get("glass_text_primary"),
                        blend(palette.get("glass_control_" + endpoint), backdrop));
                readable(palette.get("glass_text_primary"),
                        blend(palette.get("glass_surface_focus_" + endpoint), backdrop));
            }
        }
    }

    private void readable(int foreground, int surface) {
        double ratio = (luminance(blend(foreground, surface)) + 0.05) / (luminance(surface) + 0.05);
        assertTrue("Text contrast must be at least 4.5:1; got " + ratio, ratio >= 4.5);
    }

    private int blend(int foreground, int background) {
        float alpha = (foreground >>> 24) / 255f;
        int result = 0xFF000000;
        for (int shift : new int[]{16, 8, 0}) {
            int channel = Math.round(((foreground >> shift) & 255) * alpha
                    + ((background >> shift) & 255) * (1 - alpha));
            result |= channel << shift;
        }
        return result;
    }

    private double luminance(int color) {
        return linear((color >> 16) & 255) * 0.2126 + linear((color >> 8) & 255) * 0.7152
                + linear(color & 255) * 0.0722;
    }

    private double linear(int channel) {
        double normalized = channel / 255.0;
        return normalized <= 0.04045 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
