package com.dburnwal.inspectionassistant.demo;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry of all available demo inspection scenarios.
 * Add new scenarios here without touching the inspection engine.
 */
@Component
public class DemoScenarioRegistry {

    private final Map<String, InspectionScenario> scenarios;

    public DemoScenarioRegistry() {
        List<InspectionScenario> all = List.of(
                cleanCar(),
                scratchedCar(),
                dentedCar(),
                multipleDamage(),
                difficultVisibility()
        );
        this.scenarios = all.stream().collect(Collectors.toMap(InspectionScenario::id, Function.identity()));
    }

    public List<InspectionScenario> all() {
        return List.copyOf(scenarios.values());
    }

    public Optional<InspectionScenario> findById(String id) {
        return Optional.ofNullable(scenarios.get(id));
    }

    private InspectionScenario cleanCar() {
        return new InspectionScenario("CLEAN_CAR", "Clean Car",
                "A car with no visible exterior damage. Expected: no findings.",
                List.of(
                        frame(1, "clean-front",      "Front",       "demo-assets/clean-car/front.jpg"),
                        frame(2, "clean-front-left",  "Front Left",  "demo-assets/clean-car/front-left.jpg"),
                        frame(3, "clean-left-side",   "Left Side",   "demo-assets/clean-car/left-side.jpg"),
                        frame(4, "clean-rear-left",   "Rear Left",   "demo-assets/clean-car/rear-left.jpg"),
                        frame(5, "clean-rear",        "Rear",        "demo-assets/clean-car/rear.jpg"),
                        frame(6, "clean-rear-right",  "Rear Right",  "demo-assets/clean-car/rear-right.jpg"),
                        frame(7, "clean-right-side",  "Right Side",  "demo-assets/clean-car/right-side.jpg"),
                        frame(8, "clean-front-right", "Front Right", "demo-assets/clean-car/front-right.jpg")
                ));
    }

    private InspectionScenario scratchedCar() {
        return new InspectionScenario("SCRATCHED_CAR", "Scratched Car",
                "A car with visible scratches on the left door and rear bumper.",
                List.of(
                        frame(1, "scratch-front",      "Front",           "demo-assets/scratched-car/front.jpg"),
                        frame(2, "scratch-left-door",  "Left Door Scratch","demo-assets/scratched-car/left-door-scratch.jpg"),
                        frame(3, "scratch-rear",       "Rear",            "demo-assets/scratched-car/rear.jpg"),
                        frame(4, "scratch-right-side", "Right Side",      "demo-assets/scratched-car/right-side.jpg")
                ));
    }

    private InspectionScenario dentedCar() {
        return new InspectionScenario("DENTED_CAR", "Dented Car",
                "A car with a dent on the rear door and a scratch on the bumper.",
                List.of(
                        frame(1, "dent-front",          "Front",           "demo-assets/dented-car/front.jpg"),
                        frame(2, "dent-rear-door",      "Rear Door Dent",  "demo-assets/dented-car/rear-door-dent.jpg"),
                        frame(3, "dent-bumper-scratch", "Bumper Scratch",  "demo-assets/dented-car/bumper-scratch.jpg"),
                        frame(4, "dent-rear",           "Rear",            "demo-assets/dented-car/rear.jpg")
                ));
    }

    private InspectionScenario multipleDamage() {
        return new InspectionScenario("MULTIPLE_DAMAGE", "Multiple Damage",
                "A car with dents, scratches, and paint damage across multiple panels.",
                List.of(
                        frame(1, "multi-front",        "Front",          "demo-assets/dented-car/front.jpg"),
                        frame(2, "multi-rear-door",    "Rear Door",      "demo-assets/dented-car/rear-door-dent.jpg"),
                        frame(3, "multi-bumper",       "Bumper",         "demo-assets/dented-car/bumper-scratch.jpg"),
                        frame(4, "multi-left-scratch", "Left Door",      "demo-assets/scratched-car/left-door-scratch.jpg"),
                        frame(5, "multi-rear",         "Rear",           "demo-assets/dented-car/rear.jpg")
                ));
    }

    private InspectionScenario difficultVisibility() {
        return new InspectionScenario("DIFFICULT_VISIBILITY", "Difficult Visibility",
                "Frames with glare, blur, and partial views to test edge cases.",
                List.of(
                        frame(1, "diff-glare",        "Glare",        "demo-assets/difficult-car/glare.jpg"),
                        frame(2, "diff-blurry",       "Blurry",       "demo-assets/difficult-car/blurry.jpg"),
                        frame(3, "diff-partial",      "Partial View", "demo-assets/difficult-car/partial-view.jpg"),
                        frame(4, "diff-damage",       "Damage",       "demo-assets/difficult-car/damage-visible.jpg")
                ));
    }

    private InspectionScenario.DemoFrame frame(int seq, String id, String label, String path) {
        return new InspectionScenario.DemoFrame(seq, id, label, path);
    }
}
