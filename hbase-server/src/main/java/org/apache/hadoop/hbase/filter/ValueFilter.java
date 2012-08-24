/**
 * Copyright 2010 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hbase.filter;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.hbase.DeserializationException;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.FilterProtos;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This filter is used to filter based on column value. It takes an
 * operator (equal, greater, not equal, etc) and a byte [] comparator for the
 * cell value.
 * <p>
 * This filter can be wrapped with {@link WhileMatchFilter} and {@link SkipFilter}
 * to add more control.
 * <p>
 * Multiple filters can be combined using {@link FilterList}.
 * <p>
 * To test the value of a single qualifier when scanning multiple qualifiers,
 * use {@link SingleColumnValueFilter}.
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class ValueFilter extends CompareFilter {

  /**
   * Constructor.
   * @param valueCompareOp the compare op for value matching
   * @param valueComparator the comparator for value matching
   */
  public ValueFilter(final CompareOp valueCompareOp,
      final WritableByteArrayComparable valueComparator) {
    super(valueCompareOp, valueComparator);
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue v) {
    if (doCompare(this.compareOp, this.comparator, v.getBuffer(),
        v.getValueOffset(), v.getValueLength())) {
      return ReturnCode.SKIP;
    }
    return ReturnCode.INCLUDE;
  }

  public static Filter createFilterFromArguments(ArrayList<byte []> filterArguments) {
    ArrayList arguments = CompareFilter.extractArguments(filterArguments);
    CompareOp compareOp = (CompareOp)arguments.get(0);
    WritableByteArrayComparable comparator = (WritableByteArrayComparable)arguments.get(1);
    return new ValueFilter(compareOp, comparator);
  }

  /**
   * @return The filter serialized using pb
   */
  public byte [] toByteArray() {
    FilterProtos.ValueFilter.Builder builder =
      FilterProtos.ValueFilter.newBuilder();
    builder.setCompareFilter(super.convert());
    return builder.build().toByteArray();
  }

  /**
   * @param pbBytes A pb serialized {@link ValueFilter} instance
   * @return An instance of {@link ValueFilter} made from <code>bytes</code>
   * @throws DeserializationException
   * @see {@link #toByteArray()}
   */
  public static ValueFilter parseFrom(final byte [] pbBytes)
  throws DeserializationException {
    FilterProtos.ValueFilter proto;
    try {
      proto = FilterProtos.ValueFilter.parseFrom(pbBytes);
    } catch (InvalidProtocolBufferException e) {
      throw new DeserializationException(e);
    }
    final CompareOp valueCompareOp =
      CompareOp.valueOf(proto.getCompareFilter().getCompareOp().name());
    WritableByteArrayComparable valueComparator = null;
    try {
      if (proto.getCompareFilter().hasComparator()) {
        valueComparator = ProtobufUtil.toComparator(proto.getCompareFilter().getComparator());
      }
    } catch (IOException ioe) {
      throw new DeserializationException(ioe);
    }
    return new ValueFilter(valueCompareOp,valueComparator);
  }

  /**
   * @param other
   * @return true if and only if the fields of the filter that are serialized
   * are equal to the corresponding fields in other.  Used for testing.
   */
  boolean areSerializedFieldsEqual(Filter o) {
    if (o == this) return true;
    if (!(o instanceof ValueFilter)) return false;

    return super.areSerializedFieldsEqual(o);
  }
}
