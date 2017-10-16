package performance;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import com.databazoo.components.textInput.FormattedTextField;
import com.databazoo.devmodeler.tools.formatter.FormatterSQL;

import global.TestProjectSetup;

@Ignore
public class FormatterTest extends TestProjectSetup {

    private static final int LINES = 200;
    private static final int FORMAT_CYCLES = 10;
    private static final int REPEAT = 20;
    private static final int TIME_LIMIT = 15000;

    private long fastest = TIME_LIMIT;
    private long slowest = 0;
    private double sumOfSquares = 0;

    private static final String WHITESPACE = " \n\r\f\t";

    @Test
    public void compareNewLine(){
        String token = "\n";
        String token2 = " ";
        String token3 = "asdf";
        boolean contains1 = false, contains2 = false, contains3 = false;
        long totalStart = System.currentTimeMillis();
        for(int i=0; i<500000000; i++){
            contains1 = isNewLine2(token);
            contains2 = isNewLine2(token2);
            contains3 = isNewLine2(token3);
        }
        System.out.println(contains1);
        System.out.println(contains2);
        System.out.println(contains3);
        long totalTime = System.currentTimeMillis() - totalStart;
        System.out.println("Total time: " + totalTime / 1000.0 + "s");
    }

    private boolean isNewLine(String token) {
        return token.equals("\n");
    }

    private boolean isNewLine2(String token) {
        return token.length()==1 && token.charAt(0)=='\n';
    }

    @Test
    public void compareEscapes(){
        String token = "'";
        String token2 = " ";
        String token3 = "asdf";
        boolean contains1 = false, contains2 = false, contains3 = false;
        long totalStart = System.currentTimeMillis();
        for(int i=0; i<500000000; i++){
            contains1 = isEscaped3(token);
            contains2 = isEscaped3(token2);
            contains3 = isEscaped3(token3);
        }
        System.out.println(contains1);
        System.out.println(contains2);
        System.out.println(contains3);
        long totalTime = System.currentTimeMillis() - totalStart;
        System.out.println("Total time: " + totalTime / 1000.0 + "s");
    }

    private boolean isEscaped(String token) {
        return "'\"`".contains( token );
    }

    private boolean isEscaped2(String token) {
        return token.length()==1 && (token.charAt(0)=='"' || token.charAt(0)=='\'' || token.charAt(0)=='`');
    }

    private boolean isEscaped3(String token) {
        if(token.length() > 1){
            return false;
        }else{
            char char0 = token.charAt(0);
            return char0=='"' || char0=='\'' || char0=='`';
        }
    }

    @Test
    public void compareWhitespace(){
        String token = "\t";
        String token2 = " ";
        String token3 = "asdf";
        boolean contains1 = false, contains2 = false, contains3 = false;
        long totalStart = System.currentTimeMillis();
        for(int i=0; i<100000000; i++){
            contains1 = isWhitespace(token);
            contains2 = isWhitespace(token2);
            contains3 = isWhitespace(token3);
        }
        System.out.println(contains1);
        System.out.println(contains2);
        System.out.println(contains3);
        long totalTime = System.currentTimeMillis() - totalStart;
        System.out.println("Total time: " + totalTime / 1000.0 + "s");
    }
    private boolean isWhitespace(String token) {
        return WHITESPACE.contains(token);
    }
    private boolean isWhitespace2(String token) {
        if(token.length() > 1){
            return false;
        }else{
            char char0 = token.charAt(0);
            return char0 == ' ' || char0 == '\n' || char0 == '\r' || char0 == '\f' || char0 == '\t';
        }
    }

    @Test
    public void format() throws Exception {
        String sql = "SELECT *\n" +
                "FROM food.food f\n" +
                "JOIN food.food_types ft ON ft.food_id = f.id\n" +
                "WHERE\n" +
                "\t(unit != 'ml' AND type_id IN (6, 7, 9)) OR\n" +
                "\t(unit = 'ml' AND type_id NOT IN (6, 7, 9))\n" +
                "ORDER BY f.id\n" +
                "LIMIT 200;\n\n" +
                "/** EXAMPLE DATA **/\n" +
                "INSERT INTO food (food_name, carbohydrates, fat, unit, step, type_id) VALUES ( 'chicken' , 0 , 0.04 , 'g' , '10' , '2' );\n" +
                "INSERT INTO food (food_name, carbohydrates, fat, unit, step, type_id) VALUES ( 'pork' , 0 , 0.03 , 'g' , '10' , '2' );\n" +
                "SELECT DISTINCT integer FROM JOIN ON USING ORDER BY new.id OFFSET LIMIT\n\n" +
                "-- The end\n";
        StringBuilder text = new StringBuilder(sql.length()*LINES);
        for(int i = 0; i < LINES; i++){
            text.append(sql);
        }
        FormattedTextField input = new FormattedTextField(text.toString(), MyFormatterSQL.INSTANCE);

        long totalStart = System.currentTimeMillis();
        for(int j = 0; j < REPEAT; j++) {
            long start = System.currentTimeMillis();
            for (int i = 0; i < FORMAT_CYCLES; i++) {
                input.formatImmediately();
            }

            long time = System.currentTimeMillis() - start;
            System.out.println("Time: " + time / 1000.0 + "s");
            addToStats(time);

            assertTrue(time < TIME_LIMIT);
        }
        long totalTime = System.currentTimeMillis() - totalStart;

        System.out.println();
        System.out.println("Total time: " + totalTime / 1000.0 + "s");
        System.out.println("Fastest: " + fastest / 1000.0 + "s");
        System.out.println("Slowest: " + slowest / 1000.0 + "s");
        System.out.println();
        System.out.println("Average: " + totalTime/REPEAT / 1000.0 + "s");
        System.out.println("Variance: " + Math.round(Math.round(Math.sqrt(Math.abs(Math.pow((double)totalTime/(double)REPEAT, 2) - sumOfSquares/(double)REPEAT)))) / 1000.0 + "s");
        System.out.println();
        /*for(String style : FormatterBase.STYLE_USAGE.keySet()){
            System.out.println(style + ";" + STYLE_USAGE.get(style));
        }*/
    }

    private void addToStats(long time) {
        if(time < fastest){
            fastest = time;
        }
        if(time > slowest){
            slowest = time;
        }
        sumOfSquares += time * time;
    }

    private static class MyFormatterSQL extends FormatterSQL {
        private static final MyFormatterSQL INSTANCE = new MyFormatterSQL();
        private MyFormatterSQL() {
            super();
            ELEMENT_NAMES.add("food");
            ELEMENT_NAMES.add("food.food");
            ELEMENT_NAMES.add("food.food_types");
            ELEMENT_NAMES.add("food_id");
            ELEMENT_NAMES.add("id");
            ELEMENT_NAMES.add("unit");
            ELEMENT_NAMES.add("type_id");
            ELEMENT_NAMES.add("food_name");
            ELEMENT_NAMES.add("energy");
            ELEMENT_NAMES.add("protein");
            ELEMENT_NAMES.add("carbohydrates");
            ELEMENT_NAMES.add("fat");
            ELEMENT_NAMES.add("unit");
            ELEMENT_NAMES.add("step");
            DATATYPE_NAMES.add("integer");
            BEGIN_CLAUSES.add("customBegin");
            END_CLAUSES.add("customEnd");
            LOGICAL.add("customLogical");
            QUANTIFIERS.add("customQuantifier");
            DML.add("customDml");
        }
    }
}