package gsn.processor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import gsn.beans.DataField;
import gsn.beans.StreamElement;

import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

public class TestScriptletProcessor {

    private static final DataField[] dataFields1 = new DataField[]{
        new DataField("temperature","INTEGER",""),
        new DataField("speed","DOUBLE",""),
        new DataField("angle","INTEGER",""),
        new DataField("image","DOUBLE","")
    };

    private static final DataField[] dataFields2 = new DataField[]{
        new DataField("temperature","INTEGER",""),
        new DataField("speed","DOUBLE",""),
        new DataField("atm","DOUBLE","")

    };

    private static final Serializable[] data1 = new Serializable[] {
            23,
            2.34,
            -9,
            -4.5
    };

    private static final Serializable[] data2 = new Serializable[] {
            23,
            2.34,
            -4.5
    };

    @Test
    public void testCorrectProcessorParameters() {
        ScriptletProcessor processor = new ScriptletProcessor();
        TreeMap<String,String> parameters = new TreeMap<String,String>();

        boolean status = processor.initialize(dataFields1, parameters, new HashMap<ScriptletProcessor.StreamElementComparator, Binding>());
        assertFalse(status);

        parameters.put("scriptlet","println 'Hello World!';");
        status = processor.initialize(dataFields1, parameters, new HashMap<ScriptletProcessor.StreamElementComparator, Binding>());
        assertTrue(status);
    }

    @Test
    public void testStreamElementComparator () {
        ScriptletProcessor.StreamElementComparator c1 = new ScriptletProcessor.StreamElementComparator(new StreamElement(dataFields1, data1));
        assertNotNull(c1);
        ScriptletProcessor.StreamElementComparator c2 = new ScriptletProcessor.StreamElementComparator(new StreamElement(dataFields1, data1));
        assertNotNull(c2);
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));

        c1.getStreamElement().setData(0,1000);
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));

        ScriptletProcessor.StreamElementComparator c3 = new ScriptletProcessor.StreamElementComparator(new StreamElement(dataFields2, data2));
        assertNotNull(c3);
        assertFalse(c1.equals(c3));
        assertFalse(c3.equals(c1));

    }

    @Test
    public void testGroovyContextCache () {
        ScriptletProcessor processor = getProcessor(dataFields1, "println 'Hello World ' + '!'; return gsn;");
        assertNotNull(processor);
        StreamElement se = new StreamElement(dataFields1, data1);
        StreamElement se2 = new StreamElement(dataFields1, data1);
        StreamElement se3 = new StreamElement(dataFields2, data2);

        assertEquals(new ScriptletProcessor.StreamElementComparator(se), new ScriptletProcessor.StreamElementComparator(se2));
        assertNotSame(new ScriptletProcessor.StreamElementComparator(se), new ScriptletProcessor.StreamElementComparator(se3));

        Binding c1 = processor.getContext(se);
        Binding c2 = processor.getContext(se2);
        Binding c3 = processor.getContext(se3);

        assertNotNull(c1);
        assertNotNull(c2);
        assertNotNull(c3);

        HashMap<ScriptletProcessor.StreamElementComparator, Binding> cache = new HashMap<ScriptletProcessor.StreamElementComparator, Binding>();
        ScriptletProcessor.StreamElementComparator sec = new ScriptletProcessor.StreamElementComparator(se);
        ScriptletProcessor.StreamElementComparator sec2 = new ScriptletProcessor.StreamElementComparator(se2);
        ScriptletProcessor.StreamElementComparator sec3 = new ScriptletProcessor.StreamElementComparator(se3);

        assertTrue(sec.equals(sec));
        assertTrue(sec.equals(sec2));
        assertTrue(sec2.equals(sec));
        assertFalse(sec.equals(sec3));
        assertFalse(sec3.equals(sec));

        cache.put(sec, new Binding());
        
        assertTrue(cache.containsKey(sec));
        assertTrue(cache.containsKey(sec2));
        assertFalse(cache.containsKey(sec3));

    }

    @Test
    public void testCorrectScriptExecution() {

        ScriptletProcessor processor = getProcessor(dataFields1, "msg = 'Hello ' + gsn; def msg1 = 'This is a script internal variable.'");
        StreamElement se = new StreamElement(dataFields1, data1);
        Binding context = processor.getContext(se);
        context.setVariable("gsn", new String("Groovy GSN"));
        processor.evaluate(processor.script, context);
        assertNotNull(context.getVariable("msg"));
        assertEquals(context.getVariable("msg"), "Hello Groovy GSN");

        Object o = null;
        try {
            o = context.getVariable("msg1");
        }
        catch (Exception e) {}
        assertNull(o);
    }

    @Test
    public void testStatefullScriptlet() {
       ScriptletProcessor processor = getProcessor(dataFields1, "msg = (binding.getVariables().get('msg')==null) ? '' : msg; msg = 'Hello World ' + msg + ' ' + gsn + '!'; println msg; return gsn;");
        StreamElement se = new StreamElement(dataFields1, data1);
        Binding context = processor.getContext(se);
        context.setVariable("gsn", new String("Groovy GSN"));
        processor.evaluate(processor.script, context);
        assertNotNull(context.getVariable("msg"));
        assertEquals(context.getVariable("msg"), "Hello World  Groovy GSN!");

        context.setVariable("msg", new String("Stateful"));
        processor.evaluate(processor.script, context);
        assertEquals(context.getVariable("msg"), "Hello World Stateful Groovy GSN!");
    }

    @Test
    public void testBindingOut() {
        ScriptletProcessor processor = getProcessor(dataFields2, "return;");
        StreamElement se = new StreamElement(dataFields1, data1);
        Binding context = processor.getContext(se);
        processor.evaluate(processor.script, context);

        StreamElement seo = processor.formatOutputStreamElement(context);
        assertNotNull(seo.getData("temperature"));
        assertEquals(seo.getData("temperature"), data1[0]);
        assertNotNull(seo.getData("speed"));
        assertEquals(seo.getData("speed"), data1[1]);
        assertNull(seo.getData("atm"));
    }

    @Test
    public void testTimedField() {
        ScriptletProcessor processor = getProcessor(dataFields1, "return;");
        StreamElement se = new StreamElement(dataFields1, data1);
        Binding context = processor.getContext(se);
        processor.evaluate(processor.script, context);

        StreamElement seo = processor.formatOutputStreamElement(context);
        assertNotSame(seo.getTimeStamp(), 123456L);

        se.setTimeStamp(123456L);
        context = processor.getContext(se);
        processor.evaluate(processor.script, context);
        seo = processor.formatOutputStreamElement(context);
        assertEquals(123456L, seo.getTimeStamp());
    }

    @Test(expected = groovy.lang.MissingMethodException.class)
    public void testScriptletExecutionWithCompilationException() {
        ScriptletProcessor processor = getProcessor(dataFields1, "prinltn 'This Groovy code has a syntax error;'");
        StreamElement se = new StreamElement(dataFields1, data1);
        Binding context = processor.getContext(se);
        processor.evaluate(processor.script, context); 
    }

    @Test(expected = groovy.lang.MissingPropertyException.class)
    public void testScriptletExecutionWithUnsetVariableException() {
        ScriptletProcessor processor = getProcessor(dataFields1, "println 'This variable is not set ' + thevar");
        StreamElement se = new StreamElement(dataFields1, data1);
        Binding context = processor.getContext(se);
        context.setVariable("gsn", new String("Groovy GSN"));
        processor.evaluate(processor.script, context);
    }

    @Test(expected = Exception.class)
    public void testScriptletExecutionWithSyntaxError() {
        ScriptletProcessor processor = getProcessor(dataFields1, "this code is not groovy");
        StreamElement se = new StreamElement(dataFields1, data1);
        Binding context = processor.getContext(se);
        processor.evaluate(processor.script, context);
    }

    //

    private ScriptletProcessor getProcessor(DataField[] outputStructure, String scriptlet) {
        ScriptletProcessor processor = new ScriptletProcessor();
        TreeMap<String,String> parameters = new TreeMap<String,String>();
        parameters.put("scriptlet", scriptlet);
        processor.initialize(outputStructure, parameters, new HashMap<ScriptletProcessor.StreamElementComparator, Binding>());
        return processor;
    }

}
