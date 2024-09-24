package swyne;

import java.util.function.Predicate;

public interface WordRoleCondition extends Predicate<Word> {
    @Override
    public boolean test(Word word);
}
