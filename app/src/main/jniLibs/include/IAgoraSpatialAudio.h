//
//  AgoraRtcEngine SDK
//
//  Copyright (c) 2022 Agora.io. All rights reserved.
//

#ifndef AGORA_SPATIAL_AUDIO_H
#define AGORA_SPATIAL_AUDIO_H

#include <stdint.h>
#include "AgoraBase.h"
#include "IAgoraRtcEngine.h"

namespace agora {
namespace rtc {

// The information of remote voice position
struct RemoteVoicePositionInfo {
  // The coordnate of remote voice source, (x, y, z)
  float position[3];
  // The forward vector of remote voice, (x, y, z). When it's not set, the vector is forward to listner.
  float forward[3];

  RemoteVoicePositionInfo() = default;
};

/** The definition of LocalSpatialAudioConfig
 */
struct LocalSpatialAudioConfig {
  /*The reference to \ref IRtcEngine, which is the base interface class of the Agora RTC SDK and provides
   * the real-time audio and video communication functionality.
   */
  agora::rtc::IRtcEngine* rtcEngine;

  LocalSpatialAudioConfig() : rtcEngine(NULL) {}
};

/** The IBaseSpatialAudioEngine class provides the common methods of ICloudSpatialAudioEngine and ILocalSpatialAudioEngine.
 */
class IBaseSpatialAudioEngine {
 public:
  virtual ~IBaseSpatialAudioEngine() {}
  /**
   * Releases all the resources occupied by spatial audio engine.
   */
  virtual void release() = 0;
  /**
   * This method sets the maximum number of streams that a player can receive in a
   * specified audio reception range.
   *
   * @note You can call this method either before or after calling enterRoom:
   * - Calling this method before enterRoom affects the maximum number of received streams
   *   the next time the player enters a room.
   * - Calling this method after entering a room changes the current maximum number of
   *   received streams of the player.
   *
   * @param maxCount The maximum number of streams that a player can receive within
   * a specified audio reception range. If the number of receivable streams exceeds
   * the set value, the SDK receives the set number of streams closest to the player.
   *
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int setMaxAudioRecvCount(int maxCount) = 0;
  /**
   * This method sets the audio reception range. The unit of the audio reception range
   * is the same as the unit of distance in the game engine.
   *
   * @note You can call this method either before or after calling enterRoom.
   * During the game, you can call it multiple times to update the audio reception range.
   *
   * @param range The maximum audio reception range, in the unit of game engine distance.
   *
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int setAudioRecvRange(float range) = 0;

  /**
   * This method sets distance unit of game engine. The smaller the unit is, the sound fades slower
   * with distance.
   *
   * @note You can call this method either before or after calling enterRoom.
   * During the game, you can call it multiple times to update the distance unit.
   *
   * @param unit The number of meters that the game engine distance per unit is equal to. For example, setting unit as 2 means the game engine distance per unit equals 2 meters.
   *
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int setDistanceUnit(float unit) = 0;
  /**
   * Updates the position of local user.
   * When calling it in ICloudSpatialAudioEngine, it triggers the SDK to update the user position to the Agora spatial audio server. The Agora spatial audio server uses the users' world coordinates and audio reception range to determine whether they are within each other's specified audio reception range.
   * When calling it in ILocalSpatialAudioEngine, it triggers the SDK to calculate the relative position between the local and remote users and updates spatial audio parameters.
   *
   * when calling it in ICloudSpatialAudioEngine, you should notice:
   * @note
   * - Call the method after calling enterRoom.
   * - The call frequency is determined by the app. Agora recommends calling this method every
   *   120 to 7000 ms. Otherwise, the SDK may lose synchronization with the server.
   *
   * @param position The sound position of the user. The coordinate order is forward, right, and up.
   * @param axisForward The vector in the direction of the forward axis in the coordinate system.
   * @param axisRight The vector in the direction of the right axis in the coordinate system.
   * @param axisUp The vector in the direction of the up axis in the coordinate system.
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int updateSelfPosition(float position[3], float axisForward[3], float axisRight[3], float axisUp[3]) = 0;

  /**
   * Set parameters for spatial audio engine. It's deprecated for using.
   *
   * @param params The parameter string.
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int setParameters(const char* params) = 0;

  /**
   * Mute or unmute local audio stream.
   *
   * @param mute When it's false, it will send local audio stream, otherwise it will not send local audio stream.
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int muteLocalAudioStream(bool mute) = 0;
  /**
   * Mute all remote audio streams. It determines wether SDK receves remote audio streams or not.
   *
   * @param mute When it's false, SDK will receive remote audio streams, otherwise SDK will not receive remote audio streams.
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int muteAllRemoteAudioStreams(bool mute) = 0;
};

class ILocalSpatialAudioEngine : public IBaseSpatialAudioEngine {
 public:
  virtual ~ILocalSpatialAudioEngine() {}
  /**
   * Initializes the ILocalSpatialAudioEngine object and allocates the internal resources.
   *
   * @note Ensure that you call IRtcEngine::queryInterface and initialize before calling any other ILocalSpatialAudioEngine APIs.
   *
   * @param config The pointer to the LocalSpatialAudioConfig. See #LocalSpatialAudioConfig.
   *
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int initialize(const LocalSpatialAudioConfig& config) = 0;
  /**
   * Updates the position information of remote user. You should call it when remote user whose role is broadcaster moves.
   *
   * @param uid The remote user ID. It should be the same as RTC channel remote user id.
   * @param posInfo The position information of remote user. See #RemoteVoicePositionInfo.
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int updateRemotePosition(uid_t uid, const RemoteVoicePositionInfo& posInfo) = 0;
  /**
   * Remove the position information of remote user. You should call it when remote user called IRtcEngine::leaveChannel.
   *
   * @param uid The remote user ID. It should be the same as RTC channel remote user id.
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int removeRemotePosition(uid_t uid) = 0;
  /**
   * Clear the position informations of remote users.
   *
   * @return
   * - 0: Success.
   * - <0: Failure.
   */
  virtual int clearRemotePositions() = 0;
};

}  // namespace rtc
}  // namespace agora

#endif /* AGORA_SPATIAL_AUDIO_H */
