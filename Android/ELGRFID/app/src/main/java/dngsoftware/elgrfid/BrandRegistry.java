package dngsoftware.elgrfid;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BrandRegistry {

    private static final List<PrinterBrand> BRANDS =
            Collections.unmodifiableList(Arrays.asList(PrinterBrand.values()));

    private BrandRegistry() {
    }

    public static List<PrinterBrand> getAvailableBrands() {
        return BRANDS;
    }
}