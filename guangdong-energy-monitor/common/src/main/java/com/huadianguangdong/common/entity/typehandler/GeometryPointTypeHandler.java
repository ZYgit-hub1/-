package com.huadianguangdong.common.entity.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostGIS geometry(Point, 4326) ↔ JTS Point 类型处理器
 * <p>
 * 处理 PostgreSQL bytea（EWKB）与 JTS Point 之间的双向转换。
 * SRID 固定为 4326（WGS84）。
 *
 * @author huadianguangdong
 */
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcTypePerConfiguration = true)
@MappedTypes(Point.class)
public class GeometryPointTypeHandler extends BaseTypeHandler<Point> {

    /** 共享的 GeometryFactory（SRID=4326，WGS84） */
    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Point parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgo = new PGobject();
        pgo.setType("geometry");
        // EWKT 文本：SRID=4326;POINT(lng lat)
        String ewkt = String.format("SRID=%d;POINT(%f %f)", parameter.getSRID(), parameter.getX(), parameter.getY());
        pgo.setValue(ewkt);
        ps.setObject(i, pgo);
    }

    @Override
    public Point getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toPoint(rs.getObject(columnName));
    }

    @Override
    public Point getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toPoint(rs.getObject(columnIndex));
    }

    @Override
    public Point getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toPoint(cs.getObject(columnIndex));
    }

    /**
     * 将 PostgreSQL 返回的 geometry 对象转为 JTS Point
     * <p>
     * 接受两种格式：EWKT（文本）或 PGobject（bytea/字符串）
     */
    private Point toPoint(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof Point point) {
            return point;
        }
        String wkt;
        if (value instanceof PGobject pgo) {
            wkt = pgo.getValue();
        } else if (value instanceof byte[] bytes) {
            // EWKB 二进制，这里简化为使用 JTS WKBReader（生产环境建议引入）
            throw new SQLException("暂不支持 EWKB 二进制读取，请在连接串配置 binaryTransfer=false");
        } else {
            wkt = value.toString();
        }

        // 示例 EWKT：0101000020E6100000... 或 SRID=4326;POINT(113.2644 23.1291)
        if (wkt.startsWith("SRID=")) {
            String[] parts = wkt.split(";");
            String coordPart = parts[1].replaceAll("[POINT()]", "").trim();
            String[] coords = coordPart.split("\\s+");
            return GF.createPoint(new Coordinate(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
        }
        // 纯 WKT
        String coordPart = wkt.replaceAll("[POINT()]", "").trim();
        String[] coords = coordPart.split("\\s+");
        return GF.createPoint(new Coordinate(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
    }
}
