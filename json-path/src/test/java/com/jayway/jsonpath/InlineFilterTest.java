package com.jayway.jsonpath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.jsonpath.JsonPath.using;
import static com.jayway.jsonpath.TestUtils.assertHasNoResults;
import static com.jayway.jsonpath.TestUtils.assertHasOneResult;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class InlineFilterTest extends BaseTest {

    private static int bookCount = 4;

    public static final String MULTI_STORE_JSON_DOCUMENT = "{\n" +
            "   \"store\" : [{\n" +
            "      \"name\": \"First\"," +
            "      \"book\" : [\n" +
            "         {\n" +
            "            \"category\" : \"reference\",\n" +
            "            \"author\" : \"Nigel Rees\",\n" +
            "            \"title\" : \"Sayings of the Century\",\n" +
            "            \"display-price\" : 8.95\n" +
            "         },\n" +
            "         {\n" +
            "            \"category\" : \"fiction\",\n" +
            "            \"author\" : \"Evelyn Waugh\",\n" +
            "            \"title\" : \"Sword of Honour\",\n" +
            "            \"display-price\" : 12.99\n" +
            "         },\n" +
            "         {\n" +
            "            \"category\" : \"fiction\",\n" +
            "            \"author\" : \"Herman Melville\",\n" +
            "            \"title\" : \"Moby Dick\",\n" +
            "            \"isbn\" : \"0-553-21311-3\",\n" +
            "            \"display-price\" : 8.99\n" +
            "         },\n" +
            "         {\n" +
            "            \"category\" : \"fiction\",\n" +
            "            \"author\" : \"J. R. R. Tolkien\",\n" +
            "            \"title\" : \"The Lord of the Rings\",\n" +
            "            \"isbn\" : \"0-395-19395-8\",\n" +
            "            \"display-price\" : 22.99\n" +
            "         }]\n" +
            "      },\n" +
            "      {\n" +
            "       \"name\": \"Second\",\n" +
            "       \"book\": [\n" +
            "         {\n" +
            "            \"category\" : \"fiction\",\n" +
            "            \"author\" : \"Ernest Hemmingway\",\n" +
            "            \"title\" : \"The Old Man and the Sea\",\n" +
            "            \"display-price\" : 12.99\n" +
            "         }]\n" +
            "      }]}";


    public static Iterable<Configuration> configurations() {
        return Configurations.configurations();
    }


    @ParameterizedTest
    @MethodSource("configurations")
    public void root_context_can_be_referred_in_predicate(Configuration conf) {
        List<?> prices = using(conf).parse(JSON_DOCUMENT).read("store.book[?(@.display-price <= $.max-price)].display-price", List.class);

        assertThat(prices.stream().map(this::asDouble)).containsAll(asList(8.95D, 8.99D));
    }

    private Double asDouble(Object object) {
        // For json-org implementation returns a list of big decimals
        return object instanceof BigDecimal ? ((BigDecimal) object).doubleValue() : (Double) object;
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void multiple_context_object_can_be_refered(Configuration conf) {

        List all = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?(@.category == @.category) ]", List.class);
        assertThat(all.size()).isEqualTo(bookCount);

        List all2 = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?(@.category == @['category']) ]", List.class);
        assertThat(all2.size()).isEqualTo(bookCount);

        List all3 = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?(@ == @) ]", List.class);
        assertThat(all3.size()).isEqualTo(bookCount);

        List none = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?(@.category != @.category) ]", List.class);
        assertThat(none.size()).isEqualTo(0);

        List none2 = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?(@.category != @) ]", List.class);
        assertThat(none2.size()).isEqualTo(4);

    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void simple_inline_or_statement_evaluates(Configuration conf) {

        List a = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?(@.author == 'Nigel Rees' || @.author == 'Evelyn Waugh') ].author", List.class);
        assertThat(a).containsExactly("Nigel Rees", "Evelyn Waugh");

        List b = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?((@.author == 'Nigel Rees' || @.author == 'Evelyn Waugh') && @.display-price < 15) ].author", List.class);
        assertThat(b).containsExactly("Nigel Rees", "Evelyn Waugh");

        List c = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?((@.author == 'Nigel Rees' || @.author == 'Evelyn Waugh') && @.category == 'reference') ].author", List.class);
        assertThat(c).containsExactly("Nigel Rees");

        List d = using(conf).parse(JSON_DOCUMENT).read("store.book[ ?((@.author == 'Nigel Rees') || (@.author == 'Evelyn Waugh' && @.category != 'fiction')) ].author", List.class);
        assertThat(d).containsExactly("Nigel Rees");
    }


    public void no_path_ref_in_filter_hit_all() {

        List<String> res = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?('a' == 'a')].author");

        assertThat(res).containsExactly("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien");

    }

    @Test
    public void no_path_ref_in_filter_hit_none() {

        List<String> res = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?('a' == 'b')].author");

        assertThat(res).isEmpty();

    }

    @Test
    public void path_can_be_on_either_side_of_operator() {
        List<String> resLeft = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(@.category == 'reference')].author");
        List<String> resRight = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?('reference' == @.category)].author");

        assertThat(resLeft).containsExactly("Nigel Rees");
        assertThat(resRight).containsExactly("Nigel Rees");
    }

    @Test
    public void path_can_be_on_both_side_of_operator() {
        List<String> res = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(@.category == @.category)].author");

        assertThat(res).containsExactly("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien");
    }

    @Test
    public void patterns_can_be_evaluated() {
        List<String> resLeft = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(@.category =~ /reference/)].author");
        assertThat(resLeft).containsExactly("Nigel Rees");

        resLeft = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(/reference/ =~ @.category)].author");
        assertThat(resLeft).containsExactly("Nigel Rees");
    }

    @Test
    public void patterns_can_be_evaluated_with_ignore_case() {
        List<String> resLeft = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(@.category =~ /REFERENCE/)].author");
        assertThat(resLeft).isEmpty();

        resLeft = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(@.category =~ /REFERENCE/i)].author");
        assertThat(resLeft).containsExactly("Nigel Rees");
    }

    @Test
    public void patterns_match_against_lists() {
        List<String> haveRefBooks = JsonPath.parse(MULTI_STORE_JSON_DOCUMENT).read("$.store[?(@.book[*].category =~ /Reference/i)].name");
        assertThat(haveRefBooks).containsExactly("First");
    }

    @Test
    public void negate_exists_check() {
        List<String> hasIsbn = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(@.isbn)].author");
        assertThat(hasIsbn).containsExactly("Herman Melville", "J. R. R. Tolkien");

        List<String> noIsbn = JsonPath.parse(JSON_DOCUMENT).read("$.store.book[?(!@.isbn)].author");

        assertThat(noIsbn).containsExactly("Nigel Rees", "Evelyn Waugh");
    }

    @Test
    public void negate_exists_check_primitive() {
        List<Integer> ints = new ArrayList<Integer>();
        ints.add(0);
        ints.add(1);
        ints.add(null);
        ints.add(2);
        ints.add(3);


        List<Integer> hits = JsonPath.parse(ints).read("$[?(@)]");
        assertThat(hits).containsExactly(0, 1, null, 2, 3);

        hits = JsonPath.parse(ints).read("$[?(@ != null)]");
        assertThat(hits).containsExactly(0, 1, 2, 3);

        List<Integer> isNull = JsonPath.parse(ints).read("$[?(!@)]");
        assertThat(isNull).containsExactly(new Integer[]{});
        assertThat(isNull).containsExactly(new Integer[]{});
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void equality_check_does_not_break_evaluation(Configuration conf) {
        assertHasOneResult("[{\"value\":\"5\"}]", "$[?(@.value=='5')]", conf);
        assertHasOneResult("[{\"value\":5}]", "$[?(@.value==5)]", conf);

        assertHasOneResult("[{\"value\":\"5.1.26\"}]", "$[?(@.value=='5.1.26')]", conf);

        assertHasNoResults("[{\"value\":\"5\"}]", "$[?(@.value=='5.1.26')]", conf);
        assertHasNoResults("[{\"value\":5}]", "$[?(@.value=='5.1.26')]", conf);
        assertHasNoResults("[{\"value\":5.1}]", "$[?(@.value=='5.1.26')]", conf);

        assertHasNoResults("[{\"value\":\"5.1.26\"}]", "$[?(@.value=='5')]", conf);
        assertHasNoResults("[{\"value\":\"5.1.26\"}]", "$[?(@.value==5)]", conf);
        assertHasNoResults("[{\"value\":\"5.1.26\"}]", "$[?(@.value==5.1)]", conf);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void lt_check_does_not_break_evaluation(Configuration conf) {
        assertHasOneResult("[{\"value\":\"5\"}]", "$[?(@.value<'7')]", conf);

        assertHasNoResults("[{\"value\":\"7\"}]", "$[?(@.value<'5')]", conf);

        assertHasOneResult("[{\"value\":5}]", "$[?(@.value<7)]", conf);
        assertHasNoResults("[{\"value\":7}]", "$[?(@.value<5)]", conf);

        assertHasOneResult("[{\"value\":5}]", "$[?(@.value<7.1)]", conf);
        assertHasNoResults("[{\"value\":7}]", "$[?(@.value<5.1)]", conf);

        assertHasOneResult("[{\"value\":5.1}]", "$[?(@.value<7)]", conf);
        assertHasNoResults("[{\"value\":7.1}]", "$[?(@.value<5)]", conf);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void escaped_literals(Configuration conf) {
        if (conf.jsonProvider().getClass().getSimpleName().startsWith("Jackson")) {
            return;
        }
        if (conf.jsonProvider().getClass().getSimpleName().startsWith("Jakarta")) {
            // single quotes are not valid in JSON; see json.org
            return;
        }
        assertHasOneResult("[\"\\'foo\"]", "$[?(@ == '\\'foo')]", conf);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void escaped_literals2(Configuration conf) {
        if (conf.jsonProvider().getClass().getSimpleName().startsWith("Jackson")) {
            return;
        }
        assertHasOneResult("[\"\\\\'foo\"]", "$[?(@ == \"\\\\'foo\")]", conf);
    }


    @ParameterizedTest
    @MethodSource("configurations")
    public void escape_pattern(Configuration conf) {
        assertHasOneResult("[\"x\"]", "$[?(@ =~ /\\/|x/)]", conf);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void escape_pattern_after_literal(Configuration conf) {
        assertHasOneResult("[\"x\"]", "$[?(@ == \"abc\" || @ =~ /\\/|x/)]", conf);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void escape_pattern_before_literal(Configuration conf) {
        assertHasOneResult("[\"x\"]", "$[?(@ =~ /\\/|x/ || @ == \"abc\")]", conf);
    }

    @ParameterizedTest
    @MethodSource("configurations")
    public void filter_evaluation_does_not_break_path_evaluation(Configuration conf) {
        assertHasOneResult("[{\"s\": \"fo\", \"expected_size\": \"m\"}, {\"s\": \"lo\", \"expected_size\": 2}]", "$[?(@.s size @.expected_size)]", conf);
    }
}
