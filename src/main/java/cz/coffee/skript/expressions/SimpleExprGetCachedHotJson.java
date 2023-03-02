/**
 * This file is part of skJson.
 * <p>
 * Skript is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Skript is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * Copyright coffeeRequired nd contributors
 */
package cz.coffee.skript.expressions;


import ch.njol.skript.Skript;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import cz.coffee.core.cache.Cache;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;


@Since("2.5.0")
public class SimpleExprGetCachedHotJson extends SimpleExpression<JsonElement> {

    static {
        Skript.registerExpression(SimpleExprGetCachedHotJson.class, JsonElement.class, ExpressionType.SIMPLE,
                "hot(-| )cached json %string%"
        );
    }


    private Expression<String> storedKeyExpr;

    @Override
    protected @Nullable JsonElement @NotNull [] get(@NotNull Event e) {
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().serializeNulls().enableComplexMapKeySerialization().create();
        String storedKey = storedKeyExpr.getSingle(e);
        if (storedKey != null) {

            if (Cache.hotContains(storedKey)) {
                return new JsonElement[]{gson.toJsonTree(Cache.getHotPackage(storedKey).getJson())};
            }
        }
        return new JsonElement[0];
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public @NotNull Class<? extends JsonElement> getReturnType() {
        return JsonElement.class;
    }

    @Override
    public @NotNull String toString(@Nullable Event e, boolean debug) {
        return "..";
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?> @NotNull [] exprs, int matchedPattern, @NotNull Kleenean isDelayed, @NotNull ParseResult parseResult) {
        storedKeyExpr = (Expression<String>) exprs[0];
        return true;
    }
}
