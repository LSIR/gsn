package gsn.processor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

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

        boolean status = processor.initialize(dataFields1, parameters, new HashMap<ScriptletProcessor.StreamElementComparator, GroovyShell>());
        assertFalse(status);

        parameters.put("scriptlet","println 'Hello World!';");
        status = processor.initialize(dataFields1, parameters, new HashMap<ScriptletProcessor.StreamElementComparator, GroovyShell>());
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
    public void testGroovyShellCache () {
        ScriptletProcessor processor = getProcessor(dataFields1, "println 'Hello World ' + '!'; return gsn;");
        assertNotNull(processor);
        StreamElement se = new StreamElement(dataFields1, data1);
        GroovyShell shell = processor.getShell(se);
        assertNotNull(shell);
        assertTrue(shell == processor.getShell(se));
    }

    @Test
    public void testCorrectScriptExecution() {

        ScriptletProcessor processor = getProcessor(dataFields1, "msg = 'Hello ' + gsn;");
        StreamElement se = new StreamElement(dataFields1, data1);
        GroovyShell shell = processor.getShell(se);
        shell.getContext().setVariable("gsn", new String("Groovy GSN"));
        shell.evaluate(processor.scriptlet);
        assertNotNull(shell.getContext().getVariable("msg"));
        assertEquals(shell.getContext().getVariable("msg"), "Hello Groovy GSN");
        
    }

    @Test
    public void testStatefullScriptlet() {
       ScriptletProcessor processor = getProcessor(dataFields1, "msg = (binding.getVariables().get('msg')==null) ? '' : msg; msg = 'Hello World ' + msg + ' ' + gsn + '!'; println msg; return gsn;");
        StreamElement se = new StreamElement(dataFields1, data1);
        GroovyShell shell = processor.getShell(se);
        shell.getContext().setVariable("gsn", new String("Groovy GSN"));
        shell.evaluate(processor.scriptlet);
        assertNotNull(shell.getContext().getVariable("msg"));
        assertEquals(shell.getContext().getVariable("msg"), "Hello World  Groovy GSN!");

        shell.getContext().setVariable("msg", new String("Stateful"));
        shell.evaluate(processor.scriptlet);
        assertEquals(shell.getContext().getVariable("msg"), "Hello World Stateful Groovy GSN!");
    }

    @Test
    public void testBindingOut() {
        ScriptletProcessor processor = getProcessor(dataFields2, "return;");
        StreamElement se = new StreamElement(dataFields1, data1);
        GroovyShell shell = processor.getShell(se);
        shell.evaluate(processor.scriptlet);

        StreamElement seo = processor.formatOutputStreamElement(shell.getContext());
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
        GroovyShell shell = processor.getShell(se);
        shell.evaluate(processor.scriptlet);

        StreamElement seo = processor.formatOutputStreamElement(shell.getContext());
        assertNotSame(seo.getTimeStamp(), 123456L);

        se.setTimeStamp(123456L);
        shell = processor.getShell(se);
        shell.evaluate(processor.scriptlet);
        seo = processor.formatOutputStreamElement(shell.getContext());
        assertEquals(123456L, seo.getTimeStamp());
    }

    @Test(expected = groovy.lang.MissingMethodException.class)
    public void testScriptletExecutionWithCompilationException() {
        ScriptletProcessor processor = getProcessor(dataFields1, "prinltn 'This Groovy code has a syntax error;'");
        StreamElement se = new StreamElement(dataFields1, data1);
        GroovyShell shell = processor.getShell(se);
        shell.evaluate(processor.scriptlet); 
    }

    @Test(expected = groovy.lang.MissingPropertyException.class)
    public void testScriptletExecutionWithUnsetVariableException() {
        ScriptletProcessor processor = getProcessor(dataFields1, "println 'This variable is not set ' + thevar");
        StreamElement se = new StreamElement(dataFields1, data1);
        GroovyShell shell = processor.getShell(se);
        shell.getContext().setVariable("gsn", new String("Groovy GSN"));
        shell.evaluate(processor.scriptlet);
    }

    @Test(expected = Exception.class)
    public void testScriptletExecutionWithSyntaxError() {
        ScriptletProcessor processor = getProcessor(dataFields1, "this code is not groovy");
        StreamElement se = new StreamElement(dataFields1, data1);
        GroovyShell shell = processor.getShell(se);
        shell.evaluate(processor.scriptlet);
    }

    //

    private ScriptletProcessor getProcessor(DataField[] outputStructure, String scriptlet) {
        ScriptletProcessor processor = new ScriptletProcessor();
        TreeMap<String,String> parameters = new TreeMap<String,String>();
        parameters.put("scriptlet", scriptlet);
        processor.initialize(outputStructure, parameters, new HashMap<ScriptletProcessor.StreamElementComparator, GroovyShell>());
        return processor;
    }

}
