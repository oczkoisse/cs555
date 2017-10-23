package a4.chunker;

public class Size
{
    public enum Unit
    {
        K(1024), M(1024 * 1024);

        private int byteCount;

        Unit(int byteCount)
        {
            this.byteCount = byteCount;
        }

        public int getByteCount()
        {
            return byteCount;
        }

        @Override
        public String toString()
        {
            switch(this)
            {
                case K:
                    return "K";
                case M:
                    return "M";
                default:
                    return "?";
            }
        }
    }

    private int unitCount;
    private Unit unit;

    public Size(int unitCount, Unit unit)
    {
        this.unitCount = unitCount;
        this.unit = unit;
    }

    public int getByteCount()
    {
        return unitCount * unit.getByteCount();
    }

    @Override
    public String toString()
    {
        return Integer.toString(unitCount) + unit.toString();
    }
}
