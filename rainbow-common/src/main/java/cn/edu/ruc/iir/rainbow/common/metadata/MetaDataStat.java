package cn.edu.ruc.iir.rainbow.common.metadata;

import cn.edu.ruc.iir.rainbow.common.exception.MetaDataException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import parquet.hadoop.metadata.BlockMetaData;
import parquet.hadoop.metadata.ColumnChunkMetaData;
import parquet.schema.Type;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hank on 2015/1/31.
 */
public class MetaDataStat
{
    private List<MetaData> fileMetaDataList = new ArrayList<MetaData>();
    private List<Type> fields = null;
    private int columnCount = 0;
    private long rowCount = 0;
    private List<BlockMetaData> blockMetaDataList = null;

    public long getRowCount ()
    {
        if (this.rowCount <= 0)
        {
            long rowCount = 0;
            for (BlockMetaData block : getBlocks())
            {
                rowCount += block.getRowCount();
            }
            this.rowCount = rowCount;
        }
        return this.rowCount;
    }

    /**
     *
     * @param nameNode the hostname of hdfs namenode
     * @param hdfsPort the port of hdfs namenode, usually 9000 or 8020
     * @param dirPath the path of the directory which contains the parquet files, begin with /, for gen /msra/column/order/parquet/
     * @throws IOException
     * @throws MetaDataException
     */
    public MetaDataStat(String nameNode, int hdfsPort, String dirPath) throws IOException, MetaDataException
    {
        Configuration conf = new Configuration();
        FileSystem fileSystem = FileSystem.get(URI.create("hdfs://" + nameNode + ":" + hdfsPort), conf);
        Path hdfsDirPath = new Path(dirPath);
        if (! fileSystem.isFile(hdfsDirPath))
        {
            FileStatus[] fileStatuses = fileSystem.listStatus(hdfsDirPath);
            for (FileStatus status : fileStatuses)
            {
                if (! status.isDir())
                {
                    //System.out.println(status.getPath().toString());
                    this.fileMetaDataList.add(new MetaData(conf, status.getPath()));
                }
            }
        }
        if (this.fileMetaDataList.size() == 0)
        {
            throw new MetaDataException("fileMetaDataList is empty, path is not a dir.");
        }
        this.fields = this.fileMetaDataList.get(0).getFileMetaData().getSchema().getFields();
        this.columnCount = this.fileMetaDataList.get(0).getFileMetaData().getSchema().getFieldCount();
    }

    /**
     * get the blocks (row groups) of the parquet files.
     * @return
     */
    public List<BlockMetaData> getBlocks ()
    {
        if (this.blockMetaDataList == null || this.blockMetaDataList.size() == 0)
        {
            this.blockMetaDataList = new ArrayList<BlockMetaData>();
            for (MetaData meta : this.fileMetaDataList)
            {
                this.blockMetaDataList.addAll(meta.getBlocks());
            }
        }
        return this.blockMetaDataList;
    }

    public List<MetaData> getFileMetaData ()
    {
        return this.fileMetaDataList;
    }

    /**
     * get the average column chunk size of all the row groups
     * @return
     */
    public double[] getAvgColumnChunkSize ()
    {
        double[] sum = new double[this.columnCount];

        for (int i = 0; i < this.columnCount; ++i)
        {
            sum[i] = 0;
        }

        for (BlockMetaData block : getBlocks())
        {
            int i = 0;
            for (ColumnChunkMetaData column : block.getColumns())
            {
                sum[i] += column.getTotalSize();
                i++;
            }
        }

        long blockCount = this.getBlockCount();
        for (int i = 0; i < this.columnCount; ++i)
        {
            sum[i] /= blockCount;
        }

        return sum;
    }

    /**
     * get the standard deviation of the column chunk sizes.
     * @param avgSize
     * @return
     */
    public double[] getColumnChunkSizeStdDev (double[] avgSize)
    {
        double[] dev = new double[this.columnCount];

        for (int i = 0; i < this.columnCount; ++i)
        {
            dev[i] = 0;
        }

        for (BlockMetaData block : getBlocks())
        {
            int i = 0;
            for (ColumnChunkMetaData column : block.getColumns())
            {
                dev[i] += Math.pow(column.getTotalSize() - avgSize[i], 2);
                i++;
            }
        }

        long blockCount = this.getBlockCount();
        for (int i = 0; i < this.columnCount; ++i)
        {
            dev[i] = Math.sqrt(dev[i] / blockCount);
        }

        return dev;
    }

    /**
     * get the field (column) names.
     * @return
     */
    public List<String> getFieldNames ()
    {
        List<String> names = new ArrayList<String>();
        for (Type type : this.fields)
        {
            names.add(type.getName());
        }
        return names;
    }

    /**
     * get the number of blocks (row groups).
     * @return
     */
    public int getBlockCount ()
    {
        return this.getBlocks().size();
    }


    public int getFileCount ()
    {
        return this.fileMetaDataList.size();
    }

    /**
     * get the average compressed size of the rows in the parquet files.
     * @return
     */
    public double getRowSize ()
    {
        double size = 0;
        for (double columnSize : getAvgColumnChunkSize())
        {
            size += columnSize;
        }
        return size*this.getBlockCount()/this.getRowCount();
    }

    /**
     * get the total compressed size of all the parquet files.
     * @return
     */
    public double getTotalCompressedSize ()
    {
        double size = 0;
        for (BlockMetaData meta : this.getBlocks())
        {
            size += meta.getCompressedSize();
        }
        return size;
    }

    /**
     * get the total uncompressed size of the parquet files.
     * @return
     */
    public double getTotalSize ()
    {
        double size = 0;
        for (BlockMetaData meta : this.getBlocks())
        {
            size += meta.getTotalByteSize();
        }
        return size;
    }
}
