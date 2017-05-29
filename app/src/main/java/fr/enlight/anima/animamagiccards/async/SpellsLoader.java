package fr.enlight.anima.animamagiccards.async;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.enlight.anima.cardmodel.business.SpellFilterFactory;
import fr.enlight.anima.cardmodel.model.spells.SpellbookType;
import fr.enlight.anima.cardmodel.business.SpellBusinessService;
import fr.enlight.anima.cardmodel.model.spells.Spell;
import fr.enlight.anima.cardmodel.model.witchspells.Witchspells;
import fr.enlight.anima.cardmodel.model.witchspells.WitchspellsPath;

/**
 *
 */
public class SpellsLoader extends BaseLoader<List<Spell>> {

    private SpellBusinessService spellBusinessService;

    private final List<SpellFilterFactory.SpellFilter> filters;
    private final Witchspells witchspells;
    private final int bookId;

    public SpellsLoader(Context context, int bookId, List<SpellFilterFactory.SpellFilter> filters) {
        super(context);
        spellBusinessService = new SpellBusinessService(context);
        this.bookId = bookId;
        this.witchspells = null;
        this.filters = filters;
    }

    public SpellsLoader(Context context, Witchspells witchspells, List<SpellFilterFactory.SpellFilter> filters) {
        super(context);
        spellBusinessService = new SpellBusinessService(context);
        this.witchspells = witchspells;
        this.bookId = -1;
        this.filters = filters;
    }

    @Override
    public List<Spell> loadInBackground() {
        List<Spell> result = new ArrayList<>();
        if (bookId >= 0) {
            result = getBookFromIdWithType(bookId, 100);
            return filteredSpells(result);
        }

        if (witchspells != null) {
            for (WitchspellsPath witchPath : witchspells.witchPaths) {
                List<Spell> pathSpells = getBookFromIdWithType(witchPath.pathBookId, witchPath.pathLevel);

                if (witchPath.secondaryPathBookId > 0) {
                    pathSpells.addAll(getBookFromIdWithType(witchPath.secondaryPathBookId, witchPath.pathLevel));
                }

                Collections.sort(pathSpells);
                result.addAll(pathSpells);
            }

            return filteredSpells(result);
        }

        return Collections.emptyList();
    }

    private List<Spell> filteredSpells(List<Spell> spells) {
        if (filters == null) {
            return spells;
        }

        List<Spell> result = new ArrayList<>();
        for (Spell spell : spells) {
            if (isSpellFiltered(spell)) {
                result.add(spell);
            }
        }
        return result;
    }

    private boolean isSpellFiltered(Spell spell){
        for (SpellFilterFactory.SpellFilter filter : filters) {
            if(!filter.matchFilter(spell)){
                return false;
            }
        }
        return true;
    }

    private List<Spell> getBookFromIdWithType(int bookId, int levelMax) {
        List<Spell> result = new ArrayList<>();
        SpellbookType typeFromBookId = SpellbookType.getTypeFromBookId(bookId);
        for (Spell spell : spellBusinessService.getSpellsForBook(bookId)) {
            spell.spellbookType = typeFromBookId;
            result.add(spell);
            if (spell.level >= levelMax) {
                break;
            }
        }
        return result;
    }

}
