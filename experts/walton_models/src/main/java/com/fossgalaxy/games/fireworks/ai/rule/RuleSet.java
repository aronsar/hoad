package com.fossgalaxy.games.fireworks.ai.rule;

import com.fossgalaxy.games.fireworks.ai.osawa.rules.OsawaDiscard;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.TellPlayableCardOuter;
import com.fossgalaxy.games.fireworks.ai.rule.finesse.PlayFinesse;
import com.fossgalaxy.games.fireworks.ai.rule.finesse.PlayFinesseTold;
import com.fossgalaxy.games.fireworks.ai.rule.finesse.TellFinesse;
import com.fossgalaxy.games.fireworks.ai.rule.random.*;
import com.fossgalaxy.games.fireworks.ai.rule.simple.DiscardIfCertain;
import com.fossgalaxy.games.fireworks.ai.rule.simple.PlayIfCertain;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A collection of rules that can be used to construct agents.
 *
 * We actively make an effort to ensure the ordering remains the same between versions to allow agents that are built
 * using this ruleset to still function but won't always guarantee this. This is also the rule indexes used by the
 * "model" agent factory for learnt rule sets.
 *
 * Created by piers on 27/01/17.
 */
public class RuleSet {
    public static ArrayList<Rule> getRules() {
        ArrayList<Rule> rules = new ArrayList<>();

        // Discard rules
        rules.add(new DiscardRandomly());
        rules.add(new DiscardIfCertain());
        rules.add(new DiscardSafeCard());
        rules.add(new DiscardOldestFirst());
        rules.add(new DiscardUselessCard());
        rules.add(new OsawaDiscard());
        rules.add(new DiscardLeastLikelyToBeNecessary());
        rules.add(new DiscardProbablyUselessCard());

        // Tell rules
        rules.add(new TellPlayableCard());
        rules.add(new TellPlayableCardOuter());
        rules.add(new TellRandomly());
        rules.add(new TellAboutOnes());
        rules.add(new TellAnyoneAboutUsefulCard());
        rules.add(new TellUnknown());
        rules.add(new TellAnyoneAboutUselessCard());
        rules.add(new TellDispensable());
        rules.add(new TellMostInformation());


        // Play Rules
        rules.add(new PlaySafeCard());
        rules.add(new PlayIfCertain());
        rules.add(new PlayProbablySafeCard(.1));
        rules.add(new PlayProbablySafeCard(.2));
        rules.add(new PlayProbablySafeCard(.3));
        rules.add(new PlayProbablySafeCard(.4));
        rules.add(new PlayProbablySafeCard(.5));
        rules.add(new PlayProbablySafeCard(.6));
        rules.add(new PlayProbablySafeCard(.7));
        rules.add(new PlayProbablySafeCard(.8));
        rules.add(new PlayProbablySafeCard(.9));

        // new extra funtime rules
        rules.add(new TellIllInformed());
        rules.add(new TellFives());
        rules.add(new CompleteTellUsefulCard());
        rules.add(new DiscardOldestNoInfoFirst());

        rules.add(new TryToUnBlock());
        rules.add(new TellMostInformation(true));

        rules.add(new DiscardProbablyUselessCard(.1));
        rules.add(new DiscardProbablyUselessCard(.2));
        rules.add(new DiscardProbablyUselessCard(.3));
        rules.add(new DiscardProbablyUselessCard(.4));
        rules.add(new DiscardProbablyUselessCard(.5));
        rules.add(new DiscardProbablyUselessCard(.6));
        rules.add(new DiscardProbablyUselessCard(.7));
        rules.add(new DiscardProbablyUselessCard(.8));
        rules.add(new DiscardProbablyUselessCard(.9));

        //new and missing rules
        rules.add(new TellAnyoneAboutOldestUsefulCard());
        rules.add(new DiscardHighest());
        rules.add(new TellToSave());
        rules.add(new TellToSavePartialOnly());
        rules.add(new DiscardUnidentifiedCard());

        // Finesse
        rules.add(new PlayFinesse());
        rules.add(new PlayFinesseTold());
        rules.add(new TellFinesse());

        // New rules
        rules.add(new PlayUniquePossibleCard());

        return rules;
    }

    /**
     * Get a list of all rules present in the codebase which are not present in the list of permitted rules.
     *
     * This is useful for checking if any rules have been left out of the of the ruleset.
     *
     * @return any rules which are present in the framework but not in the above ruleset.
     */
    public static Collection<Class<? extends Rule>> checkRuleSet(){


        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forJavaClassPath())
                .setScanners(new SubTypesScanner())
                .setExpandSuperTypes(false)
        );
        Set<Class> rulesInRuleSet = getRules().stream().map(Object::getClass).collect(Collectors.toSet());

        Set<Class<? extends Rule>> rulesInClasspath = reflections.getSubTypesOf(Rule.class).stream()
                .filter(x -> !Modifier.isAbstract(x.getModifiers()))
                .collect(Collectors.toSet());

        rulesInClasspath.removeAll(rulesInRuleSet);
        return rulesInClasspath;
    }

    public static void main(String[] args) {
        Collection<Class<? extends Rule>> missingRules = checkRuleSet();
        missingRules.forEach(System.out::println);
    }
}
