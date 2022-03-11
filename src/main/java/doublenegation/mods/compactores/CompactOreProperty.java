package doublenegation.mods.compactores;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Slightly modified copy of net.minecraft.world.level.block.state.properties.EnumProperty */
public class CompactOreProperty<T extends Comparable<T> & StringRepresentable> extends Property<T> {
    private final ImmutableSet<T> allowedValues;
    private final Map<String, T> nameToValue = Maps.newHashMap();

    public CompactOreProperty(String name, Class<T> valueClass, Collection<T> allowedValues) {
        super(name, valueClass);
        this.allowedValues = ImmutableSet.copyOf(allowedValues);

        for(T t : allowedValues) {
            String s = ((StringRepresentable)t).getSerializedName();
            if (this.nameToValue.containsKey(s)) {
                throw new IllegalArgumentException("Multiple values have the same name '" + s + "'");
            }

            this.nameToValue.put(s, t);
        }

    }

    @Override
    public Collection<T> getPossibleValues() {
        return this.allowedValues;
    }

    @Override
    public Optional<T> getValue(String value) {
        return Optional.ofNullable(this.nameToValue.get(value));
    }

    @Override
    public String getName(T value) {
        return ((StringRepresentable)value).getSerializedName();
    }

    @Override
    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ instanceof CompactOreProperty && super.equals(p_equals_1_)) {
            CompactOreProperty<?> enumproperty = (CompactOreProperty)p_equals_1_;
            return this.allowedValues.equals(enumproperty.allowedValues) && this.nameToValue.equals(enumproperty.nameToValue);
        } else {
            return false;
        }
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        i = 31 * i + this.allowedValues.hashCode();
        i = 31 * i + this.nameToValue.hashCode();
        return i;
    }

}