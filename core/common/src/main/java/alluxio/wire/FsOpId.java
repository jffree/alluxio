/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.wire;

import alluxio.collections.Pair;
import alluxio.grpc.FsOpPId;
import alluxio.proto.journal.Journal;

import java.util.UUID;

/**
 * ID for each distinct FS operation.
 */
public class FsOpId extends Pair<Long, Long> {
  /**
   * Constructs and initializes a 128 bit FsOpId.
   *
   * @param leastSignificant the least significant bits
   * @param mostSignificant the most significant bits
   */
  public FsOpId(Long leastSignificant, Long mostSignificant) {
    super(leastSignificant, mostSignificant);
  }

  /**
   * Constructs and initializes a 128 bit FsOpId from an UUID.
   *
   * @param guid the guid
   */
  public FsOpId(UUID guid) {
    super(guid.getLeastSignificantBits(), guid.getMostSignificantBits());
  }

  /**
   * @return proto representation of fs operation id
   */
  public FsOpPId toFsProto() {
    return FsOpPId.newBuilder().setLeastSignificantBits(getFirst())
        .setMostSignificantBits(getSecond()).build();
  }

  /**
   * @return proto representation of fs operation id
   */
  public Journal.JournalOpId toJournalProto() {
    return Journal.JournalOpId.newBuilder().setLeastSignificantBits(getFirst())
        .setMostSignificantBits(getSecond()).build();
  }

  /**
   * Creates FsOpId from proto.
   *
   * @param opId proto op id
   * @return wire fs op id
   */
  public static FsOpId fromFsProto(FsOpPId opId) {
    return new FsOpId(opId.getLeastSignificantBits(), opId.getMostSignificantBits());
  }

  /**
   * Creates FsOpId from proto.
   *
   * @param opId proto op id
   * @return wire fs op id
   */
  public static FsOpId fromJournalProto(Journal.JournalOpId opId) {
    return new FsOpId(opId.getLeastSignificantBits(), opId.getMostSignificantBits());
  }
}
