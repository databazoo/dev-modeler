
package plugins.api;

import com.databazoo.devmodeler.model.Relation;

/**
 * Data window interface for plugins
 *
 * @author bobus
 */
public interface IDataWindow {
    String getQuery();

    void setQuery(String queryText);

    void appendQuery(String queryText);

    void runQuery();

    void runQuery(String sql);

    void runFocusedQuery();

    void explainQuery();

    void setQuerySelect(String columns);

    void setQueryJoin(Relation rel2, String attr2, String attr1);

    void setQueryOrder(String orderBy);

    void setQueryWhere(String where);

    IDataWindowResult getResult();

    IModelTable getTable();

    int getConnectionType();
}
