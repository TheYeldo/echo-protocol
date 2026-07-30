package dev.yeldos.echoprotocol.advancement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancementResourcesTest {
    private static final List<String> ADVANCEMENTS = List.of(
            "root", "deja_vu", "that_was_me", "it_saw_me", "corrupted_memory", "out_of_sync",
            "broken_memory", "it_looked_back", "perfect_copy", "not_me", "do_not_look_away",
            "copy_is_wrong", "familiar_face", "behind_you", "the_original", "already_home", "my_place",
            "which_one_is_real", "stop_following_me", "that_never_happened", "i_remember_it_differently",
            "you_were_never_there", "almost_lost_everything", "out_of_the_corner_of_my_eye",
            "not_my_footsteps", "the_house_remembers", "two_different_endings", "it_was_waiting_there",
            "a_pattern_emerges", "not_forgotten", "this_is_not_how_it_happened", "you_led_it_here");

    @Test
    void everyAdvancementHasAValidImpossibleCriterionParentAndBothTranslations() {
        JsonObject english = resourceJson("assets/echoprotocol/lang/en_us.json");
        JsonObject russian = resourceJson("assets/echoprotocol/lang/ru_ru.json");

        for (String id : ADVANCEMENTS) {
            JsonObject advancement = resourceJson("data/echoprotocol/advancement/" + id + ".json");
            assertEquals("minecraft:impossible",
                    advancement.getAsJsonObject("criteria").getAsJsonObject("trigger").get("trigger").getAsString(), id);
            if (!id.equals("root")) {
                String parent = advancement.get("parent").getAsString();
                assertTrue(parent.startsWith("echoprotocol:")
                        && ADVANCEMENTS.contains(parent.substring("echoprotocol:".length())), id + " -> " + parent);
            }
            assertTranslation(english, id, "title");
            assertTranslation(english, id, "description");
            assertTranslation(russian, id, "title");
            assertTranslation(russian, id, "description");
        }
    }

    private static void assertTranslation(JsonObject language, String id, String suffix) {
        String key = "advancements.echoprotocol." + id + "." + suffix;
        assertTrue(language.has(key) && !language.get(key).getAsString().isBlank(), key);
    }

    private static JsonObject resourceJson(String path) {
        var stream = AdvancementResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
