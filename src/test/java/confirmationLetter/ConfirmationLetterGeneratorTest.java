package confirmationLetter;

import dao.CurrencyDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class ConfirmationLetterGeneratorTest {

    private ConfirmationLetterGenerator confirmationLetterGenerator;

    @BeforeEach
    public void setUp()
    {
        confirmationLetterGenerator = new ConfirmationLetterGenerator();
    }

    /** It is just an example of how you can write tests in Junit. */
    @Test
    public void exampleOfTestInJunit()
    {
        CurrencyDao currencyDao = new CurrencyDao();
        confirmationLetterGenerator.setCurrencyDao(currencyDao);

        assertEquals(currencyDao, confirmationLetterGenerator.getCurrencyDao());
    }
  
}