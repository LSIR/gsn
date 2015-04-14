package ch.epfl.gsn.metadata.web.rest;

import ch.epfl.gsn.metadata.tools.taxonomy.TaxonomyResolver;
import com.google.common.base.Joiner;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Created by kryvych on 14/04/15.
 */
@Controller
@RequestMapping("/taxonomy")
public class TaxonomyController {

    private final TaxonomyResolver taxonomyResolver;

    @Inject
    public TaxonomyController(TaxonomyResolver taxonomyResolver) {
        this.taxonomyResolver = taxonomyResolver;
    }

//    @RequestMapping(value = "/columnNames", method = RequestMethod.GET)
//    public
//    @ResponseBody
//    String getColumnName(@RequestParam String tableName, @RequestParam Set<String> properties) {
//        Collection<String> names = taxonomyResolver.getColumnNameInTableForTerm(tableName, properties);
//        return Arrays.toString(names.toArray());
////        Gson gson = new Gson();
////        return gson.toJson(names);
//    }

    @RequestMapping(value = "/columnNames", method = RequestMethod.GET)
    public
    @ResponseBody
    String getColumnName(@RequestParam String property) {
        Collection<String> names = taxonomyResolver.getColumnNamesForTerm(property);
        return Joiner.on(", ").join(names);

    }

    @RequestMapping(value = "/taxonomyName", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonomyName(@RequestParam String columnName) {
        String name = taxonomyResolver.getTermForColumnName(columnName);
        return name;
    }


    @RequestMapping(value = "/terms", method = RequestMethod.GET)
    public
    @ResponseBody
    String getTaxonomyName() {
        Collection<String> terms = taxonomyResolver.getAllTerms();
        return Joiner.on(", ").join(terms);
    }



}
