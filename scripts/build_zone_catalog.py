"""Rebuild the bundled historical list from the unmodified official CSV. No network."""
import collections
import csv
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'verification/sources/iieg-oct2021/delitos_colonia_paginaweb_oct21.csv'
URL = 'https://iieg.gob.mx/ns/wp-content/uploads/2021/11/oct2021.zip'
MUNICIPALITIES = {39: 'Guadalajara', 120: 'Zapopan', 98: 'San Pedro Tlaquepaque',
    101: 'Tonalá', 97: 'Tlajomulco de Zúñiga', 70: 'El Salto', 51: 'Juanacatlán',
    44: 'Ixtlahuacán de los Membrillos', 124: 'Zapotlanejo'}
CRIMES = {'ROBO A PERSONA': 'personRobberies', 'ROBO A VEHICULOS PARTICULARES': 'vehicleRobberies'}

def display_name(raw):
    # Expand written abbreviations; do not merge distinct sections or nearby colonies.
    value = re.sub(r'^FRACC\.\s*', '', raw)
    for old, new in [('HDA.', 'HACIENDA'), ('JARDS.', 'JARDINES'), ('SECC.', 'SECCION'), ('OTE.', 'ORIENTE')]:
        value = value.replace(old, new)
    return value.title()

def build():
    counts = collections.defaultdict(collections.Counter)
    localities = collections.defaultdict(set)
    with SOURCE.open(encoding='utf-8-sig', newline='') as f:
        for row in csv.DictReader(f):
            if row['Clave_Mun'] not in {str(k) for k in MUNICIPALITIES} or row['Año'] != '2021':
                continue
            if not 1 <= int(row['Número_mes']) <= 10 or row['Delito'] not in CRIMES:
                continue
            raw = row['Colonia'].strip()
            # Do not turn missing locations or an unspecified municipal seat into a colony.
            if not raw or raw == 'N.D.' or 'CAB.' in raw or 'CABECERA' in raw:
                continue
            quantity = int(row['Cantidad'])
            assert quantity >= 0
            key = (int(row['Clave_Mun']), raw)
            counts[key][CRIMES[row['Delito']]] += quantity
            localities[key].add(row['Localidad'])
    entries = []
    coverage = {}
    for code, municipality in MUNICIPALITIES.items():
        candidates = [(key, c) for key, c in counts.items() if key[0] == code and sum(c.values()) >= 5]
        selected = sorted(candidates, key=lambda pair: (-sum(pair[1].values()), pair[0][1]))[:10]
        coverage[municipality] = len(selected)
        for rank, ((_, raw), c) in enumerate(selected, 1):
            entries.append(dict(id='iieg-2021-' + hashlib.sha256(f'{code}|{raw}'.encode()).hexdigest()[:12],
                neighborhood=display_name(raw), municipality=municipality, sourceNeighborhood=raw,
                municipalityCode=code, localities=sorted(localities[(code, raw)]),
                personRobberies=c['personRobberies'], vehicleRobberies=c['vehicleRobberies'],
                total=sum(c.values()), municipalityRank=rank, initialAction='PRECAUCION'))
    assert len({e['id'] for e in entries}) == len(entries)
    assert all(e['total'] == e['personRobberies'] + e['vehicleRobberies'] for e in entries)
    result = dict(catalogId='zmg-iieg-2021-v1', publisher='Fiscalía del Estado de Jalisco / IIEG',
        sourceUrl=URL, sourcePage='https://iieg.gob.mx/ns/?page_id=22174',
        sourceSha256=hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        periodStart='2021-01-01', periodEnd='2021-10-31', accessedOn='2026-09-12',
        selection='Hasta 10 colonias por municipio con al menos 5 registros sumados de robo a persona y a vehículos particulares en enero–octubre de 2021. Orden por cantidad descendente y nombre para desempates. Selección de la app, no clasificación oficial de peligrosidad.',
        coverage=coverage, entries=entries)
    target = ROOT / 'app/src/main/assets/zmg_historical_zones.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    lines = ['# Colonias precargadas de la ZMG', '',
        f'{len(entries)} registros derivados de [microdatos oficiales de Fiscalía / IIEG]({URL}). Periodo: enero–octubre de 2021. Consulta: 12 de septiembre de 2026.', '',
        result['selection'], '',
        'Todas empiezan en **Precaución**, por criterio inicial de la aplicación. Los conteos no están ajustados por población, tránsito o exposición de conductores. No son probabilidades ni una lista oficial de zonas prohibidas. No incluir una colonia no demuestra seguridad. Se excluyen ubicaciones desconocidas y cabeceras municipales imprecisas. Las secciones y nombres diferentes se conservan separados.', '',
        'El usuario puede editar el nivel, eliminar registros o añadir colonias. La lista histórica no caduca automáticamente. Se conserva la fecha original; instalarla hoy no actualiza los datos de 2021.', '',
        '| Municipio | Colonia en la app | Nombre en la fuente | Robo a persona | Robo a vehículo particular | Total |',
        '| --- | --- | --- | ---: | ---: | ---: |']
    for e in entries:
        lines.append(f"| {e['municipality']} | {e['neighborhood']} | {e['sourceNeighborhood']} | {e['personRobberies']} | {e['vehicleRobberies']} | {e['total']} |")
    lines += ['', '## Reproducción', '',
        '`python scripts/build_zone_catalog.py` reconstruye el JSON incluido en la APK y este listado desde el CSV descargado sin modificar.', '',
        'SHA-256 del CSV original: `' + result['sourceSha256'] + '`.', '',
        'Los nueve municipios se tomaron de [IMEPLAN](https://www.imeplan.mx/area-metropolitana-de-guadalajara/). El archivo fuente contiene años anteriores, pero esta selección solo suma enero–octubre de 2021. No se sumaron años ni periodos de otros comunicados.', '',
        'El descriptor de la Fiscalía advierte que la información puede cambiar con los resultados de las investigaciones. Se conserva el ZIP y sus descriptores en `verification/sources/` para auditoría; la APK solo incluye el catálogo agregado.', '']
    (ROOT / 'COLONIAS_ZMG.md').write_text('\n'.join(lines), encoding='utf-8')
    print(json.dumps({'entries':len(entries),'coverage':coverage,'source_sha256':result['sourceSha256']}, ensure_ascii=False))

if __name__ == '__main__':
    build()
