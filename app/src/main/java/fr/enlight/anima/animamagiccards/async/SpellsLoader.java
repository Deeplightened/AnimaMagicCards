package fr.enlight.anima.animamagiccards.async;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import fr.enlight.anima.cardmodel.business.SpellFilterFactory;
import fr.enlight.anima.cardmodel.model.spells.SpellbookType;
import fr.enlight.anima.cardmodel.business.SpellBusinessService;
import fr.enlight.anima.cardmodel.model.spells.Spell;
import fr.enlight.anima.cardmodel.model.witchspells.Witchspells;
import fr.enlight.anima.cardmodel.model.witchspells.WitchspellsPath;

import static fr.enlight.anima.animamagiccards.MainApplication.mDefSystemLanguage;

/**
 *
 */
public class SpellsLoader extends BaseLoader<List<Spell>> {

    private SpellBusinessService spellBusinessService;

    private final List<SpellFilterFactory.SpellFilter> filters;
    private final SpellFilterFactory.SpellFilter quickAccessFilter;
    private final Witchspells witchspells;
    private final int bookId;

    public SpellsLoader(Context context, int bookId, List<SpellFilterFactory.SpellFilter> filters, SpellFilterFactory.SpellFilter quickAccessFilter) {
        super(context);
        spellBusinessService = new SpellBusinessService(context);
        this.bookId = bookId;
        this.witchspells = null;
        this.filters = filters;
        this.quickAccessFilter = quickAccessFilter;
    }

    public SpellsLoader(Context context, Witchspells witchspells, List<SpellFilterFactory.SpellFilter> filters, SpellFilterFactory.SpellFilter quickAccessFilter) {
        super(context);
        spellBusinessService = new SpellBusinessService(context);
        this.witchspells = witchspells;
        this.bookId = -1;
        this.filters = filters;
        this.quickAccessFilter = quickAccessFilter;
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

                Map<Integer, Integer> freeAccessSpellsIds = witchPath.freeAccessSpellsIds;
                if (freeAccessSpellsIds != null && !freeAccessSpellsIds.isEmpty()) {
                    List<Spell> freeAccessSpells = getBookFromIdWithType(SpellbookType.FREE_ACCESS.bookId, 100);
                    for (Integer spellPosition : freeAccessSpellsIds.keySet()) {
                        int spellId = freeAccessSpellsIds.get(spellPosition);
                        for (Spell freeAccessSpell : freeAccessSpells) {
                            if (freeAccessSpell.spellId == spellId) {
                                freeAccessSpell.level = spellPosition;
                                freeAccessSpell.freeAccessAssociatedType = pathSpells.get(0).spellbookType;
                                pathSpells.add(freeAccessSpell);
                                break;
                            }
                        }
                    }
                }

                Collections.sort(pathSpells);
                result.addAll(pathSpells);
            }

            return filteredSpells(result);
        }

        return Collections.emptyList();
    }

    private List<Spell> filteredSpells(List<Spell> spells) {
        if ((filters == null || filters.isEmpty()) && quickAccessFilter == null) {
            return spells;
        }

        List<Spell> result = new ArrayList<>();
        for (Spell spell : spells) {
            boolean matching = false;

            // Quick access filter
            if (quickAccessFilter != null) {
                if (quickAccessFilter.matchFilter(spell) && isSpellMatchingFiltered(spell)) {
                    matching = true;
                }

                // Custom filters only
            } else if (isSpellMatchingFiltered(spell)) {
                matching = true;
            }

            if (matching) {
                result.add(spell);
            }
        }

        return result;
    }

    private boolean isSpellMatchingFiltered(Spell spell) {
        if (filters != null && !filters.isEmpty()) {
            for (SpellFilterFactory.SpellFilter filter : filters) {
                if (!filter.matchFilter(spell)) {
                    return false;
                }
                filter.updateSpellWithFilter(spell);
            }
        }
        return true;
    }

    private List<Spell> getBookFromIdWithType(int bookId, int levelMax) {
        List<Spell> result = new ArrayList<>();
        SpellbookType typeFromBookId = SpellbookType.getTypeFromBookId(bookId);
        List<Spell> spellsForBook = spellBusinessService.getSpellsForBook(bookId, mDefSystemLanguage);
        for (Spell spell : spellsForBook) {
            if (spell.level > levelMax) {
                break;
            }
            spell.spellbookType = typeFromBookId;
            result.add(spell);
        }
        return result;
    }

}
