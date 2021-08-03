package com.ctek.sba.util;

/**
 * Created by mfahlen on 2015-08-17.
 */
public class SecurityText {

  private static final String BULLET = "&#8226";
  private static final String EOL = "<br/>";
  private static final String TAB = "\t";

  public static final String fccStatementHeader =
      "<b>FCC & Industry Canada Statement</b>";

  public static final String fccPart15WarningEn =
      "<b>Note</b>" + EOL +
      "This device complies with Part 15 of the FCC Rules. Operation is subject to the " +
      "following two conditions: (1) this device may not cause harmful interference, " +
      "and (2) this device must accept any interference received, including interference " +
      "that may cause undesired operation." +
      EOL + EOL +
      "<b>Warning</b>" + EOL +
      "Changes or modifications to this unit not expressly approved by the party " +
      "responsible for compliance could void the user’s authority to operate the equipment.";

  // If class B
  public static final String fccPart15ClassBNoteEn =
      "<b>Note</b>"+ EOL +
      "This equipment has been tested and found to comply with the limits for a Class B " +
      "digital device, pursuant to Part 15 of the FCC Rules. These limits are designed to " +
      "provide reasonable protection against harmful interference in a residential installation. " +
      "This equipment generates, uses and can radiate radio frequency energy and, if not " +
      "installed and used in accordance with the instructions, may cause harmful interference " +
      "to radio communications." +
      EOL + EOL +
      "However, there is no guarantee that interference will not occur in a particular " +
      "installation. If this equipment does cause harmful interference to radio or television " +
      "reception, which can be determined by turning the equipment off and on, the user is " +
      "encouraged to try to correct the interference by one or more of the following measures:" +
      EOL + EOL;

  public static final String fccPart15ClassBBullet_1 =
      "Reorient or relocate the receiving antenna.";
  public static final String fccPart15ClassBBullet_2 =
      "Increase the separation between the equipment and receiver.";
  public static final String fccPart15ClassBBullet_3 =
      "Connect the equipment into an outlet on a circuit different from that to which the " +
      "receiver is connected.";
  public static final String fccPart15ClassBBullet_4 =
      "Consult the dealer or an experienced radio/TV technician for help.";

  public static final String rss210TextEn =
      "<b>Note</b>"+ EOL +
          "This device complies with Industry Canada Licence-exempt RSS-210. Operation is subject to " +
          "the following two conditions: (1) this device may not cause interference, and (2) this device must " +
          "accept any interference, including interference that may cause undesired operation of the " +
          "device." +
          EOL + EOL +
          "Le présent appareil est conforme aux CNR d'Industrie Canada applicables aux appareils radio exempts de licence. " +
          "L'exploitation est autorisée aux deux conditions suivantes : (1) l'appareil ne doit pas produire de brouillage, et " +
          "(2) l'utilisateur de l'appareil doit accepter tout brouillage radioélectrique subi, même si le brouillage est " +
          "susceptible d'en compromettre le fonctionnement.";
}
