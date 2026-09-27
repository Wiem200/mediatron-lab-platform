import PublicHeader from '../components/PublicHeader';
import { useLanguage } from '../context/LanguageContext';

const researchAreas = [
  'Mobility and QoS management in networks',
  'Telecom Services Engineering',
  'Radio network planning and optimization',
  'Radio interface optimization for mobile radio networks',
];

export default function AboutMediatron() {
  const { language } = useLanguage();
  const content = {
    en: {
      eyebrow: 'About MEDIATRON',
      title: 'Scientific leadership, network innovation, and advanced telecommunications research at SUPCOM.',
      intro:
        "Discover MEDIATRON's origins, research program, and the major scientific areas that structure the laboratory's work.",
      historyLabel: 'Laboratory History',
      historyTitle: 'From UTIC to MEDIATRON',
      historyP1:
        "The Mediatron laboratory was created in 2011, under the initiative of 9 faculty members. Previously, Mediatron represented the oldest research unit of SUPCOM, dating from 2001, and was initially called UTIC (ICT Research Unit).",
      historyP2:
        'It was directed by Professor Sami Tabbane. The laboratory direction was then ensured by Mohamed Siala, followed by Mounir Frikha, then by Belhassen Zouari from October 5, 2020, and currently by Fatma Abdelkefi since 2025.',
      timeline: 'Leadership Timeline',
      timeline2001: 'UTIC established at SUPCOM',
      timeline2011: 'MEDIATRON created by 9 faculty members',
      timeline2025: 'Directed by Fatma Abdelkefi',
      programLabel: 'Research Program',
      programTitle: 'Intelligent mobile networks, advanced radio systems, and next-generation service engineering.',
      programP1:
        'Radio-mobile networks continue to evolve and offer increasingly intelligent and resource-intensive applications. Indeed, 5G and B5G networks require new frequency bands with different radio properties and constraints. Other architectural techniques such as virtualization and Network Slicing have also been introduced in these networks to further reduce CAPEX and OPEX indicators.',
      programP2:
        '6th generation networks should include intelligent processes at the network edge (edge computing) to optimize transmission capabilities, offload transport channels, and improve QoS. Moreover, 5G/B5G/6G generations will see an increase in UAV or IoT applications applying increasingly advanced and intelligent algorithms requiring efficient data optimization and analysis.',
      areasLabel: 'Research Areas',
      areasTitle: "Core scientific themes that guide MEDIATRON's work.",
      areaPrefix: 'Area',
    },
    fr: {
      eyebrow: 'A Propos de MEDIATRON',
      title: 'Leadership scientifique, innovation reseau et recherche avancee en telecommunications a SUPCOM.',
      intro:
        "Decouvrez les origines de MEDIATRON, son programme de recherche et les grands axes scientifiques qui structurent les travaux du laboratoire.",
      historyLabel: 'Histoire du Laboratoire',
      historyTitle: 'De UTIC a MEDIATRON',
      historyP1:
        "Le laboratoire Mediatron a ete cree en 2011 a l'initiative de 9 enseignants-chercheurs. Auparavant, Mediatron representait la plus ancienne unite de recherche de SUPCOM, datant de 2001, et portait initialement le nom de UTIC (ICT Research Unit).",
      historyP2:
        "Il a ete dirige par le Professeur Sami Tabbane. La direction du laboratoire a ensuite ete assuree par Mohamed Siala, puis Mounir Frikha, ensuite Belhassen Zouari a partir du 5 octobre 2020, et actuellement par Fatma Abdelkefi depuis 2025.",
      timeline: 'Chronologie de la Direction',
      timeline2001: 'Creation de UTIC a SUPCOM',
      timeline2011: 'Creation de MEDIATRON par 9 enseignants-chercheurs',
      timeline2025: 'Direction assuree par Fatma Abdelkefi',
      programLabel: 'Programme de Recherche',
      programTitle: 'Reseaux mobiles intelligents, systemes radio avances et ingenierie des services de nouvelle generation.',
      programP1:
        "Les reseaux radio-mobiles continuent d'evoluer et offrent des applications de plus en plus intelligentes et exigeantes en ressources. Les reseaux 5G et B5G necessitent de nouvelles bandes de frequences avec des proprietes radio et des contraintes differentes. D'autres techniques architecturales comme la virtualisation et le Network Slicing ont aussi ete introduites afin de reduire davantage les indicateurs CAPEX et OPEX.",
      programP2:
        "Les reseaux de 6e generation devront integrer des processus intelligents au bord du reseau (edge computing) pour optimiser les capacites de transmission, soulager les canaux de transport et ameliorer la QoS. Les generations 5G/B5G/6G verront egalement une croissance des applications UAV et IoT utilisant des algorithmes toujours plus intelligents, necessitant une optimisation et une analyse efficaces des donnees.",
      areasLabel: 'Axes de Recherche',
      areasTitle: 'Les themes scientifiques majeurs qui guident les travaux de MEDIATRON.',
      areaPrefix: 'Axe',
    },
  }[language];

  return (
    <div className="min-h-screen bg-[#f5f2ec] text-[#1b2d52]">
      <PublicHeader />

      <main className="mx-auto max-w-7xl px-6 py-10 lg:px-10">
        <section className="rounded-[1.75rem] sm:rounded-[2.5rem] border border-[#e6dac6] bg-[radial-gradient(circle_at_top_right,_rgba(200,168,100,0.18),_transparent_32%),linear-gradient(135deg,_#1b2d52_0%,_#274071_100%)] px-8 py-12 text-white shadow-xl lg:px-12 lg:py-16">
          <p className="text-xs font-black uppercase tracking-[0.22em] sm:tracking-[0.38em] text-[#f0d898]">{content.eyebrow}</p>
          <h1 className="mt-5 max-w-3xl font-serif text-4xl leading-tight lg:text-5xl">
            {content.title}
          </h1>
          <p className="mt-5 max-w-3xl text-sm leading-8 text-[#dbe3f1] sm:text-base">
            {content.intro}
          </p>
        </section>

        <section className="mt-10 grid gap-6 lg:grid-cols-[minmax(0,1.45fr)_minmax(280px,0.85fr)]">
          <article className="rounded-[2rem] border border-[#e7decf] bg-white p-8 shadow-lg lg:p-10">
            <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.34em] text-[#b28d3e]">{content.historyLabel}</p>
            <h2 className="mt-4 font-serif text-3xl text-[#1b2d52] lg:text-4xl">{content.historyTitle}</h2>
            <div className="mt-6 space-y-5 text-sm leading-8 text-[#60708d] sm:text-base">
              <p>{content.historyP1}</p>
              <p>{content.historyP2}</p>
            </div>
          </article>

          <aside className="rounded-[2rem] border border-[#d8dfeb] bg-[#eef3fb] p-8 shadow-sm">
            <p className="text-xs font-black uppercase tracking-[0.18em] sm:tracking-[0.3em] text-[#5f7397]">{content.timeline}</p>
            <div className="mt-6 space-y-4">
              <div className="rounded-2xl bg-white/85 px-4 py-4">
                <p className="text-[11px] font-black uppercase tracking-[0.2em] text-[#8ea2c2]">2001</p>
                <p className="mt-2 text-sm font-semibold text-[#1b2d52]">{content.timeline2001}</p>
              </div>
              <div className="rounded-2xl bg-white/85 px-4 py-4">
                <p className="text-[11px] font-black uppercase tracking-[0.2em] text-[#8ea2c2]">2011</p>
                <p className="mt-2 text-sm font-semibold text-[#1b2d52]">{content.timeline2011}</p>
              </div>
              <div className="rounded-2xl bg-white/85 px-4 py-4">
                <p className="text-[11px] font-black uppercase tracking-[0.2em] text-[#8ea2c2]">2025</p>
                <p className="mt-2 text-sm font-semibold text-[#1b2d52]">{content.timeline2025}</p>
              </div>
            </div>
          </aside>
        </section>

        <section className="mt-10 rounded-[2rem] border border-[#e3dac9] bg-white p-8 shadow-lg lg:p-10">
          <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.34em] text-[#b28d3e]">{content.programLabel}</p>
          <h2 className="mt-4 font-serif text-3xl text-[#1b2d52] lg:text-4xl">
            {content.programTitle}
          </h2>
          <div className="mt-6 grid gap-5 lg:grid-cols-2">
            <p className="text-sm leading-8 text-[#60708d] sm:text-base">{content.programP1}</p>
            <p className="text-sm leading-8 text-[#60708d] sm:text-base">{content.programP2}</p>
          </div>
        </section>

        <section className="mt-10" aria-labelledby="about-research-areas-title">
          <div className="mb-8 max-w-3xl">
            <p className="text-xs font-black uppercase tracking-[0.2em] sm:tracking-[0.34em] text-[#b28d3e]">{content.areasLabel}</p>
            <h2 id="about-research-areas-title" className="mt-4 font-serif text-3xl text-[#1b2d52] lg:text-4xl">
              {content.areasTitle}
            </h2>
          </div>

          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
            {researchAreas.map((area, index) => (
              <article
                key={area}
                className="rounded-[2rem] border border-[#d8dfeb] bg-[linear-gradient(180deg,_#ffffff_0%,_#f7f9fc_100%)] p-6 shadow-lg"
              >
                <p className="text-[11px] font-black uppercase tracking-[0.22em] text-[#8ea2c2]">
                  {content.areaPrefix} {String(index + 1).padStart(2, '0')}
                </p>
                <h3 className="mt-4 font-serif text-2xl leading-tight text-[#1b2d52]">{area}</h3>
              </article>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}
