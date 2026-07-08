package dngsoftware.elgrfid;

/**
 * Brand-agnostic filament fields used by NDEF-based open standards.
 */
public final class FilamentTagData {

    public String brand = "Generic";
    public String materialType = "PLA";
    public String materialModifier = "";
    public String colorName = "";
    public String colorHex = "FF0000";
    public int nozzleMin = 190;
    public int nozzleMax = 230;
    public int bedMin = 0;
    public int bedMax = 60;
    public int diameterMicrons = 1750;
    public int weightGrams = 1000;
    public int densityMilliGramsPerCc = 1240;

    public FilamentTagData copy() {
        FilamentTagData copy = new FilamentTagData();
        copy.brand = brand;
        copy.materialType = materialType;
        copy.materialModifier = materialModifier;
        copy.colorName = colorName;
        copy.colorHex = colorHex;
        copy.nozzleMin = nozzleMin;
        copy.nozzleMax = nozzleMax;
        copy.bedMin = bedMin;
        copy.bedMax = bedMax;
        copy.diameterMicrons = diameterMicrons;
        copy.weightGrams = weightGrams;
        copy.densityMilliGramsPerCc = densityMilliGramsPerCc;
        return copy;
    }
}