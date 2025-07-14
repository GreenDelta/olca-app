#!/bin/bash

while getopts :duc flag
do
    case "${flag}" in
        d) download='true';;
        u) upload='true';;
        c) compare='true';;
    esac
done

if [ ! -z "$upload" ]; then
  ssh root@translate.openlca.org rm /opt/translator/data/previous/olca-app* -f
  ssh root@translate.openlca.org rm /opt/translator/data/previous/olca-app-osgi* -f
  ssh root@translate.openlca.org cp /opt/translator/data/original/olca-app/* /opt/translator/data/previous/olca-app
  ssh root@translate.openlca.org cp /opt/translator/data/original/olca-app-osgi/* /opt/translator/data/previous/olca-app-osgi
  scp olca-app/src/org/openlca/app/messages* root@translate.openlca.org:/opt/translator/data/original/olca-app/
  scp olca-app/OSGI-INF/l10n/bundle* root@translate.openlca.org:/opt/translator/data/original/olca-app-osgi/
fi

if [ ! -z "$download" ]; then
  scp -r root@translate.openlca.org:/opt/translator/data/olca-app/. olca-app/src/org/openlca/app/
  rm -f olca-app/src/org/openlca/app/changed.txt  
  scp -r root@translate.openlca.org:/opt/translator/data/olca-app-osgi/. olca-app/OSGI-INF/l10n/
  rm -f olca-app/OSGI-INF/l10n/changed.txt
  ssh root@translate.openlca.org mv /opt/translator/data/olca-app /opt/translator/data/original/olca-app
  ssh root@translate.openlca.org mv /opt/translator/data/olca-app-osgi /opt/translator/data/original/olca-app-osgi
fi
