package fr.enlight.anima.cardmodel.business;

import android.content.Context;

import java.util.List;

import fr.enlight.anima.cardmodel.dao.SpellDao;
import fr.enlight.anima.cardmodel.dao.SpellbookIndexResponse;
import fr.enlight.anima.cardmodel.model.spells.Spell;
import fr.enlight.anima.cardmodel.model.spells.Spellbook;
import fr.enlight.anima.cardmodel.model.spells.SpellbookResponse;
import fr.enlight.anima.cardmodel.model.spells.SpellbookType;

/**
 *
 */

public class SpellBusinessService {

    private SpellDao mSpellDao;

    private Context context;

    public SpellBusinessService(Context context) {
        this.context = context;
    }

    public List<Spell> getSpellsForBook(int bookId, String locale){
        SpellbookResponse spellbookResponse = getSpellDao().getSpellbook(context, bookId, locale);
        return spellbookResponse.spells;
    }

    public List<Spellbook> getSpellbooksIndex(String locale){
        SpellbookIndexResponse spellbookIndex = getSpellDao().getSpellbookIndex(context, locale);

        for (Spellbook spellbook : spellbookIndex.spellbooks) {
            spellbook.spellbookType = SpellbookType.getTypeFromBookId(spellbook.bookId);
        }

        return spellbookIndex.spellbooks;
    }

    public SpellDao getSpellDao() {
        if(mSpellDao == null){
            mSpellDao = new SpellDao();
        }
        return mSpellDao;
    }
}
