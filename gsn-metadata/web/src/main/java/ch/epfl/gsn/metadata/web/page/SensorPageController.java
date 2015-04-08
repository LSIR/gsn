package ch.epfl.gsn.metadata.web.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
* Created by kryvych on 01/04/15.
*/
@Controller
@RequestMapping("/web")
public class SensorPageController {

    private String message = "Hello World";

    @RequestMapping("/virtualSensor")
    public String welcome(SensorPageQuery sensorPageQuery, Model model) {
        System.out.println("sensors = " + sensorPageQuery.getSensors());
        model.addAttribute("time", new Date());
        model.addAttribute("message", this.message);
        model.addAttribute("sensors", sensorPageQuery.getSensors());
        model.addAttribute("parameters", sensorPageQuery.getParameters());

        return "sensor";
    }

    @RequestMapping("/foo")
    public String foo(Map<String, Object> model) {
        throw new RuntimeException("Foo");
    }

    @RequestMapping(value = "/hii", method = RequestMethod.GET, produces = "application/json")
    public
    @ResponseBody
    String test(Map<String, Object> model) {
        return "HIIIII";
    }

}
