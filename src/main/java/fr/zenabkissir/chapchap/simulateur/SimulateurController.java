package fr.zenabkissir.chapchap.simulateur;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SimulateurController {

    @GetMapping("/simulateur")
    public String simulateur() {
        return "simulateur";
    }
}
