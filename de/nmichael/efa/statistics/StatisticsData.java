/**
 * Title:        efa - elektronisches Fahrtenbuch für Ruderer
 * Copyright:    Copyright (c) 2001-2011 by Nicolas Michael
 * Website:      http://efa.nmichael.de/
 * License:      GNU General Public License v2
 *
 * @author Nicolas Michael
 * @version 2
 */

package de.nmichael.efa.statistics;

import java.util.Vector;

import de.nmichael.efa.data.LogbookRecord;
import de.nmichael.efa.data.PersonRecord;
import de.nmichael.efa.data.StatisticsRecord;
import de.nmichael.efa.data.efawett.Zielfahrt;
import de.nmichael.efa.data.efawett.ZielfahrtFolge;
import de.nmichael.efa.data.types.DataTypeDate;
import de.nmichael.efa.data.types.DataTypeDistance;
import de.nmichael.efa.data.types.DataTypeIntString;
import de.nmichael.efa.data.types.DataTypeTime;
import de.nmichael.efa.util.EfaUtil;
import java.util.Hashtable;
import java.util.UUID;

public class StatisticsData implements Comparable {

    public static final String SORTING_PREFIX  = "%%";
    public static final String SORTING_POSTFIX = "$$";
    public static final String SORTTOEND_PREFIX = "~END~";
    public static final String KEY_MODIFIER = "~%KEY%~";

    private StatisticsRecord sr;

    Object key;
    long lastTripTimestamp = 0;
    String sPosition;
    String sName;
    String sGender;
    String sStatus;
    String sClub;
    String sYearOfBirth;
    String sMemberNo;
    String sBoatType;
    String sDistance;
    String sRowDistance;
    String sCoxDistance;
    String sSessions;
    String sAvgDistance;
    String sDuration;
    String sDays;
    String sSpeed;
    String sDestinationAreas;
    String sWanderfahrten;
    String sDamageCount;
    String sDamageDuration;
    String sDamageAvgDuration;
    String sAdditional;
    String sComments;
    String[][] sDetailsArray;
    String sCompWarning;
    String sClubwork;
    String sClubworkTarget;
    String sClubworkRelativeToTarget;
    String sClubworkOverUnderCarryOver;
    String sClubworkCredit;

    long distance = 0;
    long rowdistance = 0;
    long coxdistance = 0;
    long count = 0;
    long avgDistance = 0;
    long duration = 0; // in minutes
    long days = 0;
    long speed = 0;
    SessionHistory sessionHistory;
    long wafaMetersSummary = 0;
    double clubwork = 0;
    double clubworkTargetHours = 0;
    double clubworkRelativeToTarget = 0;
    double clubworkOverUnderCarryOver = 0;
    double clubworkCredit = 0;

    DataTypeIntString entryNo;
    DataTypeDate date;
    DataTypeTime time; // start time for logbook records
    String[] logbookFields;
    String[] otherFields;
    CompetitionData compData;
    public Hashtable<Object,StatisticsData> matrixData;

    PersonRecord personRecord; // filled by postprocessing if this is a person
    String gender;
    boolean disabled;
    UUID boatId;

    boolean sortToEnd = false;
    int absPosition = 0;
    Vector<Zielfahrt> destinationAreaVector;
    ZielfahrtFolge destinationAreas;
    Zielfahrt[] bestDestinationAreas;
    Zielfahrt[] additionalDestinationAreas;
    
    boolean isSummary = false;
    boolean isMaximum = false;
    boolean compFulfilled = false;

    StatisticsData next; // used for chained lists of competition participants

    public StatisticsData(StatisticsRecord sr, Object key) {
        this.sr = sr;
        this.key = key;
        if (key != null && key instanceof String && ((String)key).startsWith(SORTTOEND_PREFIX)) {
            this.sortToEnd = true;
        }
    }

    public void updateSummary(StatisticsData sd) {
        this.distance += sd.distance;
        this.rowdistance += sd.rowdistance;
        this.coxdistance += sd.coxdistance;
        this.count += sd.count;
        this.duration += sd.duration;
        this.days += sd.days;
        if (sr.sIsAggrWanderfahrten) {
            this.wafaMetersSummary += CompetitionDRVFahrtenabzeichen.getWanderfahrtenMeter(sd);
        }
        if (sr.sIsAggrZielfahrten) {
            sd.getAllDestinationAreas();
            if (destinationAreas == null) {
                destinationAreas = new ZielfahrtFolge();
            }
            destinationAreas.addZielfahrten(sd.destinationAreas);
            destinationAreas.reduceToMinimun();
        }
        this.clubwork += sd.clubwork;
        this.clubworkTargetHours += sd.clubworkTargetHours;
    }

    public void updateMaximum(StatisticsData sd) {
        if (sd.distance > this.distance) {
            this.distance = sd.distance;
        }
        if (sd.rowdistance > this.rowdistance) {
            this.rowdistance = sd.rowdistance;
        }
        if (sd.coxdistance > this.coxdistance) {
            this.coxdistance = sd.coxdistance;
        }
        if (sd.count > this.count) {
            this.count = sd.count;
        }
        long myAvgDist = (sd.count > 0 ? sd.distance / sd.count : 0);
        if (myAvgDist > this.avgDistance) {
            this.avgDistance = myAvgDist;
        }
        if (sd.duration > this.duration) {
            this.duration = sd.duration;
        }
        if (sd.days > this.days) {
            this.days = sd.days;
        }
        long mySpeed = (sd.duration > 0 ? sd.distance*60 / sd.duration : 0);
        if (mySpeed > this.speed) {
            this.speed = mySpeed;
        }
        if (sd.clubwork > this.clubwork) {
            this.clubwork = sd.clubwork;
        }
    }

    public void getAllDestinationAreas() {
        destinationAreas = new ZielfahrtFolge();
        destinationAreaVector = new Vector<Zielfahrt>();
        for (int i=0; sessionHistory != null && i<sessionHistory.size(); i++) {
            LogbookRecord r = sessionHistory.get(i);
            if (r != null && r.zielfahrt != null) {
                r.zielfahrt.setDatum(r.getDate().toString());
                r.zielfahrt.setZiel(r.getDestinationAndVariantName());
                r.zielfahrt.setKm(r.getDistance().getStringValueInKilometers(true, 0, 1));
                destinationAreas.addZielfahrt(r.zielfahrt);
                destinationAreaVector.add(r.zielfahrt);
                //System.out.println(sName + ": " + r.getDate().toString() + " " +
                //        r.getDestinationAndVariantName() + " (" + r.getDistance().toString() + ") -> " + r.zielfahrt.getBereiche() );
            }
        }
        destinationAreas.reduceToMinimun();
    }

    public void createStringOutputValues(StatisticsRecord sr, int absPos, String sPosition) {
        this.absPosition = absPos;
        if (sr.sIsFieldsPosition) {
            if (!this.isMaximum && !this.isSummary && sPosition != null) {
                this.sPosition = sPosition;
            } else {
                this.sPosition = "";
            }
        }
        if (sr.sIsFieldsName && sName == null) {
            this.sName = "";
        }
        if (sr.sIsFieldsName && sName.startsWith(SORTING_PREFIX)) {
            int pos = sName.indexOf(SORTING_POSTFIX);
            if (pos > 0) {
                sName = sName.substring(pos + SORTING_POSTFIX.length());
            }
        }
        if (sr.sIsFieldsName && sName.startsWith(SORTTOEND_PREFIX)) {
            sName = sName.substring(SORTTOEND_PREFIX.length());
        }
        if (sr.sIsFieldsName && sName.indexOf(KEY_MODIFIER) > 0) {
            int pos = sName.indexOf(KEY_MODIFIER);
            sName = sName.substring(0, pos);
        }
        if (sr.sIsFieldsGender && sGender == null) {
            this.sGender = "";
        }
        if (sr.sIsFieldsStatus && sStatus == null) {
            this.sStatus = "";
        }
        if (sr.sIsFieldsClub && sClub == null) {
            this.sClub = "";
        }
        if (sr.sIsFieldsYearOfBirth && sYearOfBirth == null) {
            this.sYearOfBirth = "";
        }
        if (sr.sIsFieldsMemberNo && sMemberNo == null) {
            this.sMemberNo = "";
        }
        if (sr.sIsFieldsBoatType && sBoatType == null) {
            this.sBoatType = "";
        }
        if (sr.sIsAggrDistance) {
            int decimals = 1;
            if (sr.sTruncateDistanceToFullValue) {
                if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.list) {
                    decimals = 0;
                }
            }
            if (sr.sIgnoreNullValues && distance == 0) {
                sDistance = "";
            } else {
                this.sDistance = DataTypeDistance.getDistance(this.distance).getStringValueInDefaultUnit(sr.sDistanceWithUnit, 0, decimals);
            }
        }
        if (sr.sIsAggrRowDistance) {
            int decimals = 1;
            if (sr.sTruncateDistanceToFullValue) {
                if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.list) {
                    decimals = 0;
                }
            }
            if (sr.sIgnoreNullValues && rowdistance == 0) {
                sRowDistance = "";
            } else {
                this.sRowDistance = DataTypeDistance.getDistance(this.rowdistance).getStringValueInDefaultUnit(sr.sDistanceWithUnit, 0, decimals);
            }
        }
        if (sr.sIsAggrCoxDistance) {
            int decimals = 1;
            if (sr.sTruncateDistanceToFullValue) {
                if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.list) {
                    decimals = 0;
                }
            }
            if (sr.sIgnoreNullValues && coxdistance == 0) {
                sCoxDistance = "";
            } else {
                this.sCoxDistance = DataTypeDistance.getDistance(this.coxdistance).getStringValueInDefaultUnit(sr.sDistanceWithUnit, 0, decimals);
            }
        }
        if (sr.sIsAggrSessions) {
            if (sr.sIgnoreNullValues && count == 0) {
                this.sSessions = "";
            } else {
                this.sSessions = Long.toString(this.count);
            }
        }
        if (sr.sIsAggrAvgDistance) {
            if (this.count > 0) {
                this.avgDistance = this.distance / this.count;
                this.sAvgDistance = DataTypeDistance.getDistance(this.avgDistance).getStringValueInDefaultUnit(sr.sDistanceWithUnit, 1, 1);
            } else {
                this.avgDistance = 0;
                this.sAvgDistance = "";
            }
        }
        if (sr.sIsAggrDuration) {
            if (sr.sIgnoreNullValues && duration == 0) {
                this.sDuration = "";
            } else {
                this.sDuration = EfaUtil.getHHMMstring(duration) +
                        (sr.sDistanceWithUnit ? " h" : "");
            }
        }
        if (sr.sIsAggrDays) {
            if (sr.sIgnoreNullValues && count == 0) {
                this.sDays = "";
            } else {
                this.sDays = Long.toString(days);
            }
        }
        if (sr.sIsAggrSpeed) {
            if (this.duration > 0) {
                this.speed = this.distance*60 / this.duration;
                this.sSpeed = DataTypeDistance.getDistance(this.speed).getStringValueInDefaultUnit(sr.sDistanceWithUnit, 1, 1)
                        + (sr.sDistanceWithUnit ? "/h" : "");
            } else {
                this.speed = 0;
                this.sSpeed = "";
            }
        }
        if (sr.sIsAggrDamageCount) {
            if (sr.sIgnoreNullValues && count == 0) {
                this.sDamageCount = "";
            } else {
                this.sDamageCount = Long.toString(this.count);
            }
        }
        if (sr.sIsAggrDamageDuration) {
            if (sr.sIgnoreNullValues && count == 0) {
                this.sDamageDuration = "";
            } else {
                this.sDamageDuration = Long.toString(this.days);
            }
        }
        if (sr.sIsAggrDamageAvgDuration) {
            if (sr.sIgnoreNullValues && count == 0) {
                this.sDamageAvgDuration = "";
            } else {
                this.sDamageAvgDuration = EfaUtil.zehntelInt2String((int)(days*10/count));
            }
        }
        if (sr.sIsAggrZielfahrten) {
            if (destinationAreas == null) {
                getAllDestinationAreas();
            }
            if (!isSummary) {
                sDestinationAreas = destinationAreas.toString();
            } else {
                sDestinationAreas = destinationAreas.getUniqueAres();
            }
        }
        if (sr.sIsAggrWanderfahrten) {
            int decimals = 1;
            if (sr.sTruncateDistanceToFullValue) {
                if (sr.sStatisticCategory == StatisticsRecord.StatisticCategory.list) {
                    decimals = 0;
                }
            }
            long meters = CompetitionDRVFahrtenabzeichen.getWanderfahrtenMeter(this);
            if (isSummary) {
                meters = wafaMetersSummary;
            }
            if (sr.sIgnoreNullValues && meters == 0) {
                sWanderfahrten = "";
            } else {
                sWanderfahrten = DataTypeDistance.getDistance(meters).
                        getStringValueInDefaultUnit(sr.sDistanceWithUnit, 0, decimals);
            }
        }

        this.clubworkOverUnderCarryOver = this.clubwork - this.clubworkTargetHours;
        double t_hours = sr.sTransferableClubworkHours;
        if(this.clubworkOverUnderCarryOver < - t_hours) {
            this.clubworkOverUnderCarryOver += t_hours;
        }
        else if(this.clubworkOverUnderCarryOver > t_hours) {
            this.clubworkOverUnderCarryOver -= t_hours;
        }
        else {
            this.clubworkOverUnderCarryOver = 0;
        }

        if (sr.sIsAggrClubwork) {
            if (sr.sOnlyMembersWithInsufficientClubwork && clubworkOverUnderCarryOver == 0) {
                this.sClubwork = "";
            } else {
                this.sClubwork = this.clubwork+"";
            }
        }
        if (sr.sIsAggrClubworkTarget) {
            if (sr.sOnlyMembersWithInsufficientClubwork && clubworkOverUnderCarryOver == 0) {
                this.sClubworkTarget = "";
            } else {
                this.sClubworkTarget = Math.round(this.clubworkTargetHours*100)/100d+"";
            }
        }
        if (sr.sIsAggrClubworkRelativeToTarget) {
            if (sr.sOnlyMembersWithInsufficientClubwork && clubworkOverUnderCarryOver == 0) {
                this.sClubworkRelativeToTarget = "";
            } else {
                this.clubworkRelativeToTarget = this.clubwork - this.clubworkTargetHours;
                this.sClubworkRelativeToTarget = ""+Math.round(this.clubworkRelativeToTarget*100)/100d;
            }
        }
        if (sr.sIsAggrClubworkOverUnderCarryOver) {
            if (this.isSummary || (sr.sOnlyMembersWithInsufficientClubwork && clubworkOverUnderCarryOver == 0)) {
                this.sClubworkOverUnderCarryOver = "";
            } else {
                this.sClubworkOverUnderCarryOver = ""+Math.round(this.clubworkOverUnderCarryOver*100)/100d;
            }
        }
        if (sr.sIsAggrClubworkCredit) {
            if (this.isSummary || (sr.sOnlyMembersWithInsufficientClubwork && clubworkOverUnderCarryOver == 0)) {
                this.sClubworkCredit = "";
            } else {
                this.sClubworkCredit = ""+this.clubworkCredit;
            }
        }
    }

    public int compareTo(Object o) {
        return compareTo(o, true);
    }

    public int compareTo(Object o, boolean withSecondCriterion) {
        StatisticsData osd = (StatisticsData)o;

        int order = (sr.sSortingOrderAscending ? 1 : -1);

        // first check whether we have
        if (this.sortToEnd != osd.sortToEnd) {
            if (this.sortToEnd) {
                return 1;
            } else {
                return -1;
            }
        }

        switch(sr.sSortingCriteria) {
            case distance:
                if (this.distance > osd.distance) {
                    return 1 * order;
                } else if (this.distance < osd.distance) {
                    return -1 * order;
                }
                break;
            case rowdistance:
                if (this.rowdistance > osd.rowdistance) {
                    return 1 * order;
                } else if (this.rowdistance < osd.rowdistance) {
                    return -1 * order;
                }
                break;
            case coxdistance:
                if (this.coxdistance > osd.coxdistance) {
                    return 1 * order;
                } else if (this.coxdistance < osd.coxdistance) {
                    return -1 * order;
                }
                break;
            case sessions:
            case damageCount:
                if (this.count > osd.count) {
                    return 1 * order;
                } else if (this.count < osd.count) {
                    return -1 * order;
                }
                break;
            case avgDistance:
                if (this.avgDistance > osd.avgDistance) {
                    return 1 * order;
                } else if (this.avgDistance < osd.avgDistance) {
                    return -1 * order;
                }
                break;
            case duration:
                if (this.duration > osd.duration) {
                    return 1 * order;
                } else if (this.duration < osd.duration) {
                    return -1 * order;
                }
                break;
            case days:
            case damageDuration:
                if (this.days > osd.days) {
                    return 1 * order;
                } else if (this.days < osd.days) {
                    return -1 * order;
                }
                break;
            case speed:
                if (this.speed > osd.speed) {
                    return 1 * order;
                } else if (this.speed < osd.speed) {
                    return -1 * order;
                }
                break;
            case name:
                if (this.sName != null && osd.sName != null) {
                    int res = this.sName.compareTo(osd.sName);
                    if (res != 0) {
                        return res * order;
                    }
                }
                break;
            case gender:
                if (this.sGender != null && osd.sGender != null) {
                    int res = this.sGender.compareTo(osd.sGender);
                    if (res != 0) {
                        return res * order;
                    }
                }
                break;
            case status:
                if (this.sStatus != null && osd.sStatus != null) {
                    int res = this.sStatus.compareTo(osd.sStatus);
                    if (res != 0) {
                        return res * order;
                    }
                }
                break;
            case yearOfBirth:
                if (this.sYearOfBirth != null && osd.sYearOfBirth != null) {
                    int res = EfaUtil.string2int(this.sYearOfBirth, 0) -
                              EfaUtil.string2int(osd.sYearOfBirth, 0);
                    if (res != 0) {
                        return res * order;
                    }
                }
                break;
            case memberNo:
                if (this.sMemberNo != null && osd.sMemberNo != null) {
                    int no1 = EfaUtil.string2int(this.sMemberNo, 0);
                    int no2 = EfaUtil.string2int(osd.sMemberNo, 0);
                    int res = no1 - no2;
                    if (no1 == 0 && no2 == 0) {
                        res = this.sMemberNo.compareTo(osd.sMemberNo);
                    }
                    if (res != 0) {
                        return res * order;
                    }
                }
                break;
            case boatType:
                if (this.sBoatType != null && osd.sBoatType != null) {
                    int res = this.sBoatType.compareTo(osd.sBoatType);
                    if (res != 0) {
                        return res * order;
                    }
                }
                break;
            case entryNo:
                if (this.entryNo != null && osd.entryNo != null) {
                    int res = this.entryNo.compareTo(osd.entryNo);
                    if (res != 0) {
                        return res * order;
                    }
                }
                break;
            case date:
                if (this.date != null && osd.date != null) {
                    int res = this.date.compareTo(osd.date);
                    if (res != 0) {
                        return res * order;
                    } else {
                        if (this.time != null && osd.time != null) {
                            res = this.time.compareTo(osd.time);
                            if (res != 0) {
                                return res * order;
                            }
                        }
                    }
                }
                break;
            case damageAvgDuration:
                float thisDur = (this.count > 0 ? (float)this.days / (float)this.count : 0);
                float osdDur = (osd.count > 0 ? (float)osd.days / (float)osd.count : 0);
                if (thisDur > osdDur) {
                    return 1 * order;
                } else if (thisDur < osdDur) {
                    return -1 * order;
                }
                break;
            case clubwork:
                if (this.clubwork > osd.clubwork) {
                    return 1 * order;
                } else if (this.clubwork < osd.clubwork) {
                    return -1 * order;
                }
                break;
        }

        if (withSecondCriterion && this.sName != null && osd.sName != null) {
            return this.sName.compareTo(osd.sName);
        }
        return 0;

    }

    Object getSortingValue() {
        switch(sr.sSortingCriteria) {
            case distance:
                return distance;
            case rowdistance:
                return rowdistance;
            case coxdistance:
                return coxdistance;
            case sessions:
            case damageCount:
                return count;
            case avgDistance:
                return avgDistance;
            case duration:
                return duration;
            case days:
            case damageDuration:
                return days;
            case speed:
                return speed;
            case name:
                return sName;
            case gender:
                return sGender;
            case status:
                return sStatus;
            case yearOfBirth:
                return sYearOfBirth;
            case memberNo:
                return sMemberNo;
            case boatType:
                return sBoatType;
            case entryNo:
                return entryNo;
            case date:
                return date;
            case damageAvgDuration:
                return (this.count > 0 ? (float)this.days / (float)this.count : 0);
            case clubwork:
                return clubwork;
        }
        return null;
    }
    
    String getPositionString(StatisticsData[] arr, int idx) {
        Object prev = (idx>0 ? arr[idx-1].getSortingValue() : null);
        if (prev != null && prev.equals(getSortingValue())) {
            return arr[idx-1].getPositionString(arr, idx-1);
        } else {
            return Integer.toString(idx + 1);
        }
    }
    
}
