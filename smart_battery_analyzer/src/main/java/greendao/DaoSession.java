package greendao;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import greendao.Device;
import greendao.Voltage;

import greendao.DeviceDao;
import greendao.VoltageDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig deviceDaoConfig;
    private final DaoConfig voltageDaoConfig;

    private final DeviceDao deviceDao;
    private final VoltageDao voltageDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        deviceDaoConfig = daoConfigMap.get(DeviceDao.class).clone();
        deviceDaoConfig.initIdentityScope(type);

        voltageDaoConfig = daoConfigMap.get(VoltageDao.class).clone();
        voltageDaoConfig.initIdentityScope(type);

        deviceDao = new DeviceDao(deviceDaoConfig, this);
        voltageDao = new VoltageDao(voltageDaoConfig, this);

        registerDao(Device.class, deviceDao);
        registerDao(Voltage.class, voltageDao);
    }
    
    public void clear() {
        deviceDaoConfig.getIdentityScope().clear();
        voltageDaoConfig.getIdentityScope().clear();
    }

    public DeviceDao getDeviceDao() {
        return deviceDao;
    }

    public VoltageDao getVoltageDao() {
        return voltageDao;
    }

}
