package greendao;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by evgeny.akhundzhanov on 12.04.2018.
 */

public class DBUpgrades {


  class DBUpgrade {

    int     version;
    String  query;

    DBUpgrade (int version, String query) {
      this.version = version;
      this.query = query;
    }

  } // EOClass DBUpgrade

  private List<DBUpgrade> list;

  DBUpgrades () {
    list = new ArrayList<>();

    // v. 5
    String upgradeQuery51 = "ALTER TABLE " + VoltageDao.TABLENAME + " ADD COLUMN " + VoltageDao.Properties.Temperature.columnName + " REAL default '47'";
    list.add(new DBUpgrade(5, upgradeQuery51));

    return;
  }

  void applyChanges4Version (SQLiteDatabase db, int iOldVersion) {
    for(DBUpgrade upgrade : list) {
      if(upgrade.version > iOldVersion) {
        db.execSQL(upgrade.query);
        Log.i("DaoMaster", "Upgrade from v. " + iOldVersion + " to v. " + upgrade.version + " -  SUCCESS");
        Log.i("DaoMaster", "Upgrade query: " + upgrade.query);
      }
      else {
        // do nothing
      }
    }
    return;
  }

} // EOClass DBUpgrades
