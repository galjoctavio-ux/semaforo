"""Reproduce the supplement from manually checked, explicitly cited municipal tables.
No scraping Facebook, inferred aliases, combined periods, or model-generated place names.
"""
import hashlib
import json
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
READ_ON = '2026-09-12'
entries = {}

def norm(s):
    return ''.join(c for c in unicodedata.normalize('NFKD', s.lower()) if not unicodedata.combining(c))

def add(place, municipality, publisher, title, period, finding, url, community=False):
    key = norm(place + '|' + municipality)
    if key not in entries:
        entries[key] = dict(id='zmg-supp-' + hashlib.sha256(key.encode()).hexdigest()[:16], neighborhood=place, municipality=municipality, references=[])
    entries[key]['references'].append(dict(kind='REPORTE COMUNITARIO SIN CORROBORAR' if community else 'FUENTE OFICIAL', publisher=publisher, title=title, period=period, finding=finding, url=url))

for place in ['La Calma','Arboledas','Las Águilas','Santa Margarita','La Tuzanía','Arcos de Zapopan','Lomas de Zapopan','Colinas del Rey','Tabachines']:
    add(place,'Zapopan','Gobierno Municipal de Zapopan','Operativo Policía Cercana','Publicación sin fecha visible; no se asume actual',
        'Colonia nombrada en el refuerzo de vigilancia dirigido a lugares con mayor incidencia. Sin conteo por colonia ni tasa de riesgo.',
        'https://www.zapopan.gob.mx/v3/noticias/comisaria-de-zapopan-refuerza-la-vigilancia-con-operativo-policia-cercana')

c4 = [('San Agustín',66),('Santa Cruz de las Flores',39),('Hacienda Santa Fe',37),('San Sebastián el Grande',33),('La Tijera',29),('Nueva Galicia',28),('El Zapote',25),('Lomas del Sur',25),('Chulavista',23),('Tulipanes',19),('Santa Cruz del Valle',18),('El Refugio',17),('Los Cántaros',15),('Real del Valle',14),('Lomas de San Agustín',13),('Paseo de los Agaves',13),('Real del Sol',13),('Bosques de Santa Anita',12),('Colinas del Roble',12),('Villa California',12),('Los Eucaliptos',11),('Villa Fontana Aqua',11),('El Palomar',10),('Paseos del Valle',10)]
for place,count in c4:
    add(place,'Tlajomulco de Zúñiga','Ayuntamiento de Tlajomulco / C4','Disminuye C4 delitos en Tlajomulco','2020-01-01 a 2021-02-14; publicado 2021-02-23',
        f'{count} incidencias de robo de vehículos particulares en el listado municipal. Cantidad absoluta de ese periodo; no sumar con IIEG 2021.',
        'https://www.tlajomulco.gob.mx/node/8068')

add('Quintas del Valle','Tlajomulco de Zúñiga','Ayuntamiento de Tlajomulco','Intervienen colonias como parte del Plan de Seguridad','Publicado 2024-10-27',
    'Colonia nombrada en intervención del plan que prioriza áreas con mayor incidencia. No aporta una tasa de riesgo para choferes.',
    'https://www.tlajomulco.gob.mx/comunicacion-institucional/intervienen-colonias-de-tlajo-como-parte-del-plan-de-seguridad')

for place in ['Jalisco','Loma Dorada']:
    add(place,'Tonalá','Ayuntamiento de Tonalá','Programa Municipal de Prevención 2022–2024 · PDF página 48, EI-2','Tabla: fuente municipal 2021; programa 2022–2024',
        'Una de las dos colonias que la tabla identifica con mayor incidencia delictiva. Loma Dorada se nombra sin sección; no se extrapolan los conteos de secciones A/B.',
        'https://tonala.gob.mx/portal/wp-content/uploads/2025/10/Prevencion-de-la-violencia-TNL-22-24.pdf#page=48')

for place,pct in [('Zona Centro','13'),('Revolución','11'),('Santa María Tequepexpan','10.7'),('San Pedrito','10.6'),('San Martín de las Flores','6')]:
    add(place,'San Pedro Tlaquepaque','Comisaría de Tlaquepaque','Plan Integral 2015–2018 · PDF página 7 (impresa 6), sección 3.5','Plan 2015–2018; tabla sin periodo propio explícito',
        f'{pct}% en atención de servicios de la tabla de colonias con mayor incidencia. Es distribución de servicios, no probabilidad de asalto. Nombre de fuente: '+('Fraccionamiento Revolución' if place=='Revolución' else place)+'.',
        'https://transparencia.tlaquepaque.gob.mx/wp-content/uploads/2016/01/transparencia_pablo.pdf#page=7')

# Public original report reached from the group; no private author, plate, exact address or contact is copied.
add('Portillo López','San Pedro Tlaquepaque','Reporte público enlazado desde el grupo de choferes Guadalajara','Relato de asalto y robo de vehículo','Fecha de publicación no legible; comentarios fechados 2026-07-29; consultado 2026-09-12',
    'Relato de un asalto durante la espera de viaje en esta colonia, por la noche. Sin corroboración oficial. Un incidente no estima la frecuencia del delito. Mantener como precaución editable.',
    'https://www.facebook.com/KJLUAR/posts/pfbid02A9iqxovSUBdrKYg3NetGsnnHYmoZQneniZdFYuHKbr1wsJGewEpg8HBx8GoKaggCl',True)

catalog = dict(schema=1, version='0.3.0', consultedOn=READ_ON, entries=list(entries.values()), exclusions=['Cabecera Municipal: ubicación imprecisa','Otros: no es colonia','Pobreza, salud mental y consumo de sustancias por sí solos: no determinan riesgo al conducir'])
path=ROOT/'app/src/main/assets/zmg_zone_supplement.json'
path.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

base=json.loads((ROOT/'app/src/main/assets/zmg_historical_zones.json').read_text(encoding='utf-8'))['entries']
def matchkey(e):
    place=norm(e['neighborhood']).removeprefix('fraccionamiento ')
    muni=norm(e['municipality']).replace('san pedro tlaquepaque','tlaquepaque')
    return place+'|'+muni
keys={matchkey(e) for e in base}
new=[e for e in entries.values() if matchkey(e) not in keys]
print(json.dumps(dict(base=len(base),supplementPlaces=len(entries),references=sum(len(e['references']) for e in entries.values()),new=len(new),freshTotal=len(keys|{matchkey(e) for e in entries.values()}),newPlaces=[e['neighborhood']+' / '+e['municipality'] for e in new]),ensure_ascii=False,indent=2))

doc=['# Fuentes adicionales de colonias ZMG · 0.3.0','',f'Consulta: {READ_ON}. Base original de 64 reglas conservada. Instalación nueva: {len(keys|{matchkey(e) for e in entries.values()})} reglas. El complemento contiene {len(entries)} lugares y {sum(len(e["references"]) for e in entries.values())} referencias; {len(new)} lugares se añaden a la base original.','',
     'Todos los lugares añadidos empiezan en **Precaución**. El nivel lo decide la app y el usuario puede eliminarlo o editarlo. Las fuentes no asignan una probabilidad de peligro a choferes. Cada periodo se muestra por separado; no se suman los datos del C4 con los de Fiscalía/IIEG. No se infieren equivalencias entre El Zapote y Zapote del Valle, ni entre Zona Centro y Centro.','',
     'Al actualizar desde 0.2.1 se conservan costos, niveles, horarios y vigencias; las colonias de la base original eliminadas no se restauran. Si la lista ya estaba vacía, sigue vacía. Cuando hay una lista, se añaden los lugares nuevos una vez. Vaciar o eliminar después de esta actualización no provoca recarga.','',
     '| Colonia | Municipio | Referencias y hallazgo |','|---|---|---|']
for e in entries.values():
    refs='; '.join(f'[{r["publisher"]}]({r["url"]}): {r["period"]}. {r["finding"]}' for r in e['references'])
    doc.append(f'| {e["neighborhood"]} | {e["municipality"]} | {refs} |')
doc += ['','## Límites','',
        'La detección exige nombre completo de colonia y municipio legibles; no usa polígonos ni evalúa calles intermedias. La ausencia de coincidencia no significa seguridad. Los conteos absolutos dependen del tamaño de la colonia, afluencia y denuncias.','',
        'El reporte comunitario proviene de una publicación pública consultada desde el grupo privado autorizado. No se copiaron placas, domicilios precisos, nombres de víctimas ni teléfonos. Se verificó la existencia del relato, no el delito. No se transforma automáticamente en nivel Alto.','',
        'Las páginas 7 de Tlaquepaque y 48 de Tonalá se comprobaron visualmente. Copias y renders en verification/sources, fuera del APK. El APK contiene únicamente el catálogo agregado. Reproducción: `python scripts/build_zone_supplement.py`.']
(ROOT/'FUENTES_COLONIAS.md').write_text('\n'.join(doc)+'\n',encoding='utf-8')
